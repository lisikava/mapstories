tileLayerURL = 'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png'

window.onload = async function () {
    clearAllFields();
    await loadPins();
    await loadCategories();
}

async function loadCategories() {
    try {
        const response = await fetch('/categories.json');
        categoriesData = await response.json();
    } catch (error) {
        console.error('Failed to load categories:', error);
    }
}

var map = L.map('map', {
    center: [50.065870, 19.934727],
    zoom: 12,
    zoomControl: false,
    attributionControl: false
});

L.tileLayer(tileLayerURL, { maxZoom: 19 }).addTo(map);

const pinIcon = L.icon({
    iconUrl: './assets/pin-3.svg',
    iconSize: [40, 40],
});

const reportIcon = L.icon({
    iconUrl: './assets/incident-1.svg',
    iconSize: [40, 40],
});

function createColoredPinIcon(color) {
    const svgContent = `
        <svg
            xmlns="http://www.w3.org/2000/svg"
            width="800"
            height="800"
            viewBox="0 0 48 48"
            fill="${color}"
        >
            <rect width="48" height="48" fill="none"/>
            <path d="M24,4a12,12,0,0,0-2,23.8V42a2,2,0,0,0,4,0V27.8A12,12,0,0,0,24,4Zm0,16a4,4,0,1,1,4-4A4,4,0,0,1,24,20Z"/>
        </svg>
    `.trim();

    const blob = new Blob([svgContent], { type: 'image/svg+xml' });
    const url = URL.createObjectURL(blob);

    const icon = L.icon({
        iconUrl: url,
        iconSize: [40, 40],
        iconAnchor: [20, 40],
        popupAnchor: [0, -40]
    });

    return icon;
}

// table of pin icons for different pin categories
// maps category to svg icon
const pinIconTable = {
    '': createColoredPinIcon('#b79171'),  // default
    'event': createColoredPinIcon('#d51fd8'),
    'found': createColoredPinIcon('#1f7ed8'),
    'lost': createColoredPinIcon('#6e58a2'),
    'report': reportIcon,
    'story': createColoredPinIcon('#834913')
}

function getIconForCategory(category) {
    let current = category;
    while (current !== '') {
        if (pinIconTable.hasOwnProperty(current)) {
            return pinIconTable[current];
        }
        const lastDotIndex = current.lastIndexOf('.');
        if (lastDotIndex === -1) break;
        current = current.substring(0, lastDotIndex);
    }
    return pinIconTable[''];
}

const style = document.createElement('style');
style.textContent = `
    .description-input-group input.error {
        border: 2px solid #ff4444;
        background-color: rgba(255, 0, 0, 0.05);
    }
    .search-input-outline.typing {
        border: 2px solid #ff4444 !important;
    }
    .simple-search-text.typing {
        border: 2px solid #ff4444 !important;
    }
    .tag-input.typing {
        border: 2px solid #ff4444 !important;
    }
    .description-input.typing {
        border: 2px solid #ff4444 !important;
    }
`;
document.head.appendChild(style);

const pinsDescriptionContainer = document.getElementById('pins-description-container');
const descriptionContainer = document.getElementById('description-container');
const addCellButton = document.getElementById('add-cell');
const submitDescriptionButton = document.getElementById('submit-description');
const cancelDescriptionButton = document.getElementById('cancel-description');
const searchContainer = document.getElementById('search-container');
const advancedSearchForm = document.getElementById('advanced-search-form');
const searchCancelButton = document.getElementById('search-cancel');
const searchSubmitButton = document.getElementById('search-submit');
const categoryContainer = document.getElementById('category-container');
const tagsContainer = document.getElementById('tags-container');

let currentPin;
let createFormOpen = false;
let editFormOpen = false;
let searchFormOpen = false;
const allDescriptions = [];
const placedPins = [];
let categoriesData = {};

function removeListeners() {
    addCellButton.removeEventListener('click', addDescriptionHandler);
    submitDescriptionButton.removeEventListener('click', submitDescriptionHandler);
    cancelDescriptionButton.removeEventListener('click', cancelDescriptionHandler);
}

function addListeners() {
    addCellButton.addEventListener('click', addDescriptionHandler);
    submitDescriptionButton.addEventListener('click', submitDescriptionHandler);
    cancelDescriptionButton.addEventListener('click', cancelDescriptionHandler);
}

function clearAllFields() {
    document.querySelector(".simple-search-text").value = "";
    document.getElementById('search-bbox-input').value = "";
    document.getElementById('search-after-input').value = "";
    document.getElementById('search-email-input').value = "";
    
    // Clear category inputs appended to search container
    const categoryRows = searchContainer.querySelectorAll('.search-input-row.category-row');
    categoryRows.forEach(row => row.remove());
    
    // Clear tags container
    clearTagsContainer();
}

function clearTagsContainer() {
    const rows = tagsContainer.querySelectorAll('.search-input-row');
    // Keep only the first row, clear its values
    if (rows.length > 0) {
        const firstRow = rows[0];
        const tagInput = firstRow.querySelector('.search-tag-input');
        const descInput = firstRow.querySelector('.search-description-input');
        if (tagInput) {
            tagInput.value = "";
        }
        if (descInput) {
            descInput.value = "";
        }
        // Remove additional rows
        for (let i = 1; i < rows.length; i++) {
            rows[i].remove();
        }
    }
}
// Create recommended fields for category
function createCategorySpecificFields(category) {
    const existingElements = descriptionContainer.querySelectorAll('.category-specific-element');
    existingElements.forEach(el => el.remove());
    
    const categoryKey = category.toLowerCase();
    const categoryConfig = categoriesData[categoryKey];
    
    if (!categoryConfig) return;
    
    const firstInputGroup = descriptionContainer.querySelector('.description-input-group');
    if (!firstInputGroup) return;
    
    // Create is_vague category info box
    if (categoryConfig.is_vague) {
        const vagueCategoryBox = document.createElement('div');
        vagueCategoryBox.classList.add('category-info-box', 'category-specific-element', 'vague-category-box');
        vagueCategoryBox.innerHTML = `${category.charAt(0).toUpperCase() + category.slice(1)} is a general category, please provide more detailed category`;
        vagueCategoryBox.style.cssText = `
            background-color: white;
            padding: 15px;
            border-radius: 8px;
            margin-bottom: 10px;
            text-align: center;
            color: #333;
            font-size: 14px;
            width: 304px;
            box-sizing: border-box;
        `;
        firstInputGroup.insertAdjacentElement('afterend', vagueCategoryBox);
    }
    // Create expected content info box 
    if (categoryConfig.expected && categoryConfig.expected.length > 0) {
        const expectedBox = document.createElement('div');
        expectedBox.classList.add('category-info-box', 'category-specific-element', 'expected-content-box');
        expectedBox.innerHTML = `Expected content: ${categoryConfig.expected.join(', ')}`;
        expectedBox.style.cssText = `
            background-color: white;
            padding: 15px;
            border-radius: 8px;
            margin-bottom: 10px;
            text-align: center;
            color: #333;
            font-size: 14px;
            width: 304px;
            box-sizing: border-box;
        `;
        const insertAfter = descriptionContainer.querySelector('.vague-category-box') || firstInputGroup;
        insertAfter.insertAdjacentElement('afterend', expectedBox);
    }
}

function setupCategoryInputListener() {
    const categoryInput = descriptionContainer.querySelector('.category-input-group .description-input');
    if (categoryInput) {
        categoryInput.addEventListener('input', function() {
            const category = this.value.trim().toLowerCase();
            if (category !== '') {
                const mainCategory = category.split('.')[0];
                createCategorySpecificFields(mainCategory);
                setupTagDescriptionListener(mainCategory);
            } else {
                const existingElements = descriptionContainer.querySelectorAll('.category-specific-element');
                existingElements.forEach(el => el.remove());
            }
        });
        
        categoryInput.addEventListener('focus', function() {
            this.classList.add('typing');
        });
        
        categoryInput.addEventListener('blur', function() {
            this.classList.remove('typing');
        });
    }
}

function setupTagDescriptionListener(category) {
    const observer = new MutationObserver(function(mutations) {
        mutations.forEach(function(mutation) {
            if (mutation.type === 'childList') {
                mutation.addedNodes.forEach(function(node) {
                    if (node.nodeType === 1 && node.classList && node.classList.contains('description-input-group')) {
                        const tagInput = node.querySelector('.tag-input');
                        const descInput = node.querySelector('.description-input');
                        if (tagInput && descInput) {
                            setupInputListener(tagInput, descInput, category);
                        }
                    }
                });
            }
        });
    });
    
    observer.observe(descriptionContainer, { childList: true, subtree: true });
    
    const existingInputGroups = descriptionContainer.querySelectorAll('.description-input-group');
    existingInputGroups.forEach(group => {
        const tagInput = group.querySelector('.tag-input');
        const descInput = group.querySelector('.description-input');
        if (tagInput && descInput && !tagInput.disabled) {
            setupInputListener(tagInput, descInput, category);
        }
    });
}

function setupInputListener(tagInput, descInput, category) {
    const checkForVagueCategory = () => {
        const categoryKey = category.toLowerCase();
        const categoryConfig = categoriesData[categoryKey];
        
        if (categoryConfig && categoryConfig.is_vague) {
            const allInputGroups = descriptionContainer.querySelectorAll('.description-input-group');
            let hasSpecificDetails = false;
            
            allInputGroups.forEach(group => {
                const tag = group.querySelector('.tag-input');
                const desc = group.querySelector('.description-input');
                if (tag && desc && !tag.disabled && tag.value.trim() !== '' && desc.value.trim() !== '') {
                    hasSpecificDetails = true;
                }
            });
            
             const vagueCategoryBox = descriptionContainer.querySelector('.vague-category-box');
             if (hasSpecificDetails && vagueCategoryBox) {
                 vagueCategoryBox.style.display = 'none';
             } else if (!hasSpecificDetails && vagueCategoryBox) {
                 vagueCategoryBox.style.display = 'block';
             }
        }
    };
    
    tagInput.addEventListener('input', checkForVagueCategory);
    descInput.addEventListener('input', checkForVagueCategory);
    
    tagInput.addEventListener('focus', function() {
        this.classList.add('typing');
    });
    
    tagInput.addEventListener('blur', function() {
        this.classList.remove('typing');
    });
    
    descInput.addEventListener('focus', function() {
        this.classList.add('typing');
    });
    
    descInput.addEventListener('blur', function() {
        this.classList.remove('typing');
    });
}

const addDescriptionHandler = function () {
    const newDescriptionInput = document.createElement('div');
    newDescriptionInput.classList.add('description-input-group');
    newDescriptionInput.innerHTML = `
        <input type="text" class="tag-input" placeholder="Tag">
        <input type="text" class="description-input" placeholder="Enter description">
    `;
    descriptionContainer.appendChild(newDescriptionInput);
};

const submitDescriptionHandler = async function () {
    if (editFormOpen) {
        await submitEditHandler();
    } else {
        await submitCreateHandler();
    }
};

const submitCreateHandler = async function () {
    const descriptionGroups = descriptionContainer.querySelectorAll('.description-input-group');
    let hasEmptyFields = false;

    descriptionGroups.forEach(group => {
        const tagInput = group.querySelector('.tag-input');
        const descriptionInput = group.querySelector('.description-input');
        tagInput.classList.remove('error');
        descriptionInput.classList.remove('error');

        if (!tagInput.disabled && tagInput.value.trim() === '') {
            tagInput.classList.add('error');
            hasEmptyFields = true;
        }
        if (descriptionInput.value.trim() === '') {
            descriptionInput.classList.add('error');
            hasEmptyFields = true;
        }
    });

    if (hasEmptyFields) {
        return;
    }

    const currentPinTagsAndDescriptions = Array.from(descriptionGroups).map(pair => {
        const tagInput = pair.querySelector('.tag-input');
        const descriptionInput = pair.querySelector('.description-input');
        const tag = tagInput.value.trim();
        const description = descriptionInput.value.trim();
        return { tag: tag, description: description };
    });

    const categoryObj = currentPinTagsAndDescriptions.find(pair => pair.tag === "Category");
    const category = categoryObj ? categoryObj.description : "";
    const tags = {};
    currentPinTagsAndDescriptions.forEach(pair => {
        if (pair.tag !== "Category") {
            tags[pair.tag] = pair.description;
        }
    });

    const pinData = { location: { lat: currentPin.getLatLng().lat, lon: currentPin.getLatLng().lng }, category, tags };
        try {
        const response = await fetch('/pins', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(pinData)
        });
        const result = await response.json();

        const newIcon = getIconForCategory(category);
        currentPin.setIcon(newIcon);

        placedPins.push({
            pin: currentPin,
            tagsAndDescriptions: currentPinTagsAndDescriptions,
            id: result.id
        });

        let popupContent = buildPopUpContent(currentPinTagsAndDescriptions, placedPins.length - 1);
        currentPin.bindPopup(popupContent).openPopup();
        setupPopupButtonEvents(currentPin, placedPins.length - 1);
    } catch (err) {
        console.error('Failed to create pin:', err);
    }

    pinsDescriptionContainer.classList.add('hidden');
    
    // Show search container when pin form is closed (unless advanced search is open)
    if (!searchFormOpen) {
        searchContainer.classList.remove('hidden');
    }
    
    currentPin = null;
    createFormOpen = false;
    removeListeners();
};

const submitEditHandler = async function () {
    const pinInfo = placedPins.find(info => info.pin === currentPin);
    if (!pinInfo || pinInfo.id === undefined) {
        console.error("Cannot edit pin: no ID found.");
        return;
    }

    const descriptionGroups = descriptionContainer.querySelectorAll('.description-input-group');
    let hasEmptyFields = false;

    descriptionGroups.forEach(group => {
        const tagInput = group.querySelector('.tag-input');
        const descriptionInput = group.querySelector('.description-input');
        tagInput.classList.remove('error');
        descriptionInput.classList.remove('error');

        if (!tagInput.disabled && tagInput.value.trim() === '') {
            tagInput.classList.add('error');
            hasEmptyFields = true;
        }
        if (descriptionInput.value.trim() === '') {
            descriptionInput.classList.add('error');
            hasEmptyFields = true;
        }
    });

    if (hasEmptyFields) return;

    const updatedTagsAndDescriptions = Array.from(descriptionGroups).map(pair => {
        return {
            tag: pair.querySelector('.tag-input').value.trim(),
            description: pair.querySelector('.description-input').value.trim()
        };
    });

    const categoryObj = updatedTagsAndDescriptions.find(pair => pair.tag === "Category");
    const category = categoryObj ? categoryObj.description : "";
    const tags = {};
    updatedTagsAndDescriptions.forEach(pair => {
        if (pair.tag !== "Category") {
            tags[pair.tag] = pair.description;
        }
    });

    const updateData = {
        id: pinInfo.id,
        location: {
            lat: currentPin.getLatLng().lat,
            lon: currentPin.getLatLng().lng
        },
        category,
        tags
    };

    try {
        const response = await fetch(`/pins/${pinInfo.id}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(updateData)
        });

        if (response.ok) {
            const newIcon = getIconForCategory(category);
            currentPin.setIcon(newIcon);

            pinInfo.tagsAndDescriptions = updatedTagsAndDescriptions;
            const newPopup = buildPopUpContent(updatedTagsAndDescriptions, placedPins.indexOf(pinInfo));
            currentPin.bindPopup(newPopup).openPopup();
        } else {
            console.error("Failed to update pin.");
        }
    } catch (err) {
        console.error('Error during pin update:', err);
    }

    pinsDescriptionContainer.classList.add('hidden');
    
    // Show search container when pin form is closed (unless advanced search is open)
    if (!searchFormOpen) {
        searchContainer.classList.remove('hidden');
    }
    
    currentPin = null;
    createFormOpen = false;
    editFormOpen = false;
    removeListeners();
};

const cancelDescriptionHandler = function () {
    if (createFormOpen && !editFormOpen) {
        map.removeLayer(currentPin);
        currentPin = null;
    }
    pinsDescriptionContainer.classList.add('hidden');
    
    // Show search container when pin form is closed (unless advanced search is open)
    if (!searchFormOpen) {
        searchContainer.classList.remove('hidden');
    }
    
    createFormOpen = false;
    editFormOpen = false;
    removeListeners();
};

function buildPopUpContent(tagsAndDescriptions, pinIndex) {
    const container = document.createElement('div');
    tagsAndDescriptions.forEach(pair => {
        const tagElement = document.createElement('b');
        tagElement.textContent = `${pair.tag}: `;
        const descText = document.createTextNode(pair.description);
        const lineDiv = document.createElement('div');
        lineDiv.appendChild(tagElement);
        lineDiv.appendChild(descText);
        const hr = document.createElement('hr');
        container.appendChild(lineDiv);
        container.appendChild(hr);
    });
    const buttonContainer = document.createElement('div');
    buttonContainer.style.display = 'flex';
    buttonContainer.style.justifyContent = 'flex-end';
    buttonContainer.style.gap = '8px';

    const editButton = document.createElement('button');
    editButton.textContent = 'Edit';
    editButton.className = 'pin-edit-button';
    editButton.dataset.pinIndex = pinIndex;
    buttonContainer.appendChild(editButton);

    const deleteButton = document.createElement('button');
    deleteButton.textContent = 'Delete';
    deleteButton.className = 'pin-delete-button';
    deleteButton.dataset.pinIndex = pinIndex;
    buttonContainer.appendChild(deleteButton);

    container.appendChild(buttonContainer);
    return container;
}

function setupPopupButtonEvents(marker, pinIndex) {
    marker.on('popupopen', function () {
        const popupElem = document.querySelector('.leaflet-popup-content');
        if (!popupElem) return;
        const editButton = popupElem.querySelector('.pin-edit-button');
        const deleteButton = popupElem.querySelector('.pin-delete-button');
        if (editButton) {
            editButton.onclick = function () {
                EditPin(pinIndex);
            };
        }
        if (deleteButton) {
            deleteButton.onclick = function () {
                deletePin(pinIndex);
            };
        }
    });
}

function EditPin(pinIndex) {
    editFormOpen = true;
    const pinInfo = placedPins[pinIndex];
    const tagsAndDescriptions = pinInfo.tagsAndDescriptions;
    if (!pinInfo) return;
    
    // Hide search container when edit form is active
    searchContainer.classList.add('hidden');
    
    pinsDescriptionContainer.classList.remove('hidden');
    createFormOpen = true;
    currentPin = pinInfo.pin;

    descriptionContainer.innerHTML = '';
    
    let category = '';
    if (tagsAndDescriptions && tagsAndDescriptions.length > 0) {
        const categoryPair = tagsAndDescriptions.find(pair => pair.tag === "Category");
        if (categoryPair) {
            const fullCategory = categoryPair.description.toLowerCase();
            category = fullCategory.split('.')[0];
            console.log('Found full category:', fullCategory);
            console.log('Main category for lookup:', category);
        }
    }
    
    if (tagsAndDescriptions && tagsAndDescriptions.length > 0) {
        // Create the input fields
        tagsAndDescriptions.forEach(pair => {
            const descriptionDiv = document.createElement('div');
            descriptionDiv.classList.add('description-input-group');
            if (pair.tag === "Category") {
                descriptionDiv.classList.add('category-input-group');
            }
            descriptionDiv.innerHTML = `
                <input type="text" class="tag-input" value="${pair.tag}" ${pair.tag === "Category" ? "disabled" : ""}>
                <input type="text" class="description-input" value="${pair.description}">
            `;
            descriptionContainer.appendChild(descriptionDiv);
        });
    }
    
    if (category !== '') {
        createCategorySpecificFields(category);
        setupTagDescriptionListener(category);
    }
    addListeners();
    setupCategoryInputListener();
}

async function deletePin(pinIndex) {
    const confirmed = confirm("Are you sure want to remove this pin?");
    if (!confirmed) return;
    const pinInfo = placedPins[pinIndex];
    try {
        const response = await fetch(`/pins/${pinInfo.id}`, {
            method: 'DELETE',
        });
        if (response.ok) {
            map.removeLayer(pinInfo.pin);
            placedPins.splice(pinIndex, 1);
            placedPins.forEach((info, index) => {
                const newPopup = buildPopUpContent(info.tagsAndDescriptions, index);
                info.pin.bindPopup(newPopup);
                setupPopupButtonEvents(info.pin, index);
            });
            map.closePopup();
        } else {
            console.error("Failed to delete pin.");
        }
    } catch (err) {
        console.error("Error deleting pin:", err);
    }
}

function displayPinContent(tagsAndDescriptions) {
    descriptionContainer.innerHTML = '';
    if (tagsAndDescriptions && tagsAndDescriptions.length > 0) {
        tagsAndDescriptions.forEach(desc => {
            const descriptionDiv = document.createElement('div');
            descriptionDiv.classList.add('description-input-group');
            descriptionDiv.innerHTML = `<b>${pair.tag}:</b> ${pair.description}`;
            descriptionContainer.appendChild(descriptionDiv);
        });
        pinsDescriptionContainer.classList.remove('hidden');
    } else {
        pinsDescriptionContainer.classList.add('hidden');
    }
}

map.on('click', function (e) {
    if (searchFormOpen)
        return;

    let pinClicked = false;
    placedPins.forEach(pinInfo => {
        if (e.layerPoint && pinInfo.pin.getLatLng().equals(e.latlng)) {
            displayPinContent(pinInfo.tagsAndDescriptions);
            pinClicked = true;
        }
    });

    if (!pinClicked && !createFormOpen) {
        createFormOpen = true;
        editFormOpen = false;
        
        // Hide search container when pin creation form is active
        searchContainer.classList.add('hidden');
        
        pinsDescriptionContainer.classList.remove('hidden');
        descriptionContainer.innerHTML = `
                    <div class="description-input-group category-input-group">
                        <input type="text" class="tag-input" disabled value="Category">
                        <input type="text" class="description-input" placeholder="Enter category">
                   </div>
                `;

        descriptionContainer.scrollTop = 0;
        currentPin = L.marker(e.latlng, { icon: pinIcon }).addTo(map);
        addListeners();
        setupCategoryInputListener();
    } else if (!pinClicked && createFormOpen) {
        pinsDescriptionContainer.classList.add('hidden');
        
        // Show search container when pin creation form is closed (unless advanced search is open)
        if (!searchFormOpen) {
            searchContainer.classList.remove('hidden');
        }
        
        createFormOpen = false;
        if (currentPin && !editFormOpen) {
            map.removeLayer(currentPin);
            currentPin = null;
            removeListeners();
        }
    }

});

async function loadPins() {
    try {
        var bounds = map.getBounds();
        var bbox = getCurrentBounds();
        const response = await fetch(`/pins?bbox=${encodeURIComponent(bbox)}`);
        const pins = await response.json();
        displayPins(pins);
        map.fitBounds(bounds);
        // map.setView([50.065870, 19.934727], 12);
    } catch (error) {
        console.error('Failed to load pins:', error);
    }
}

// Function that opens the Advanced Search Form
function openSearchForm() {
    const searchButtonsContainer = document.querySelector('.search-buttons-container');

    if (searchFormOpen) {
        advancedSearchForm.classList.add('hidden');
        searchButtonsContainer.classList.remove('active');
        searchContainer.classList.remove('advanced-active');
        searchFormOpen = false;
    } else {
        // If pin creation form is open, close it first
        if (createFormOpen) {
            pinsDescriptionContainer.classList.add('hidden');
            createFormOpen = false;
            if (currentPin && !editFormOpen) {
                map.removeLayer(currentPin);
                currentPin = null;
            }
            // Show search container since pin form is closed
            searchContainer.classList.remove('hidden');
        }
        
        document.querySelector(".simple-search-text").value = "";
        var currentBounds = getCurrentBounds();
        document.getElementById('default-bbox-input').value = currentBounds;
        advancedSearchForm.classList.remove('hidden');
        searchButtonsContainer.classList.add('active');
        searchContainer.classList.add('advanced-active');
        searchFormOpen = true;
    }
}

function getCurrentBounds() {
    return [map.getBounds().getSouthWest().lat, map.getBounds().getSouthWest().lng, map.getBounds().getNorthEast().lat, map.getBounds().getNorthEast().lng];
}

// Function that closes the Advanced Search Form 
function closeSearchForm() {
    const searchButtonsContainer = document.querySelector('.search-buttons-container');

    advancedSearchForm.classList.add('hidden');
    searchButtonsContainer.classList.remove('active');
    searchContainer.classList.remove('advanced-active');
    searchFormOpen = false;
}

// Function that handles the search submit
async function handleSearchSubmit() {
    advancedSearch();
    closeSearchForm();
}

// Function to add new category input row to search container
function addCategoryInputRow(afterRow = null) {
    const newRow = document.createElement('div');
    newRow.classList.add('search-input-row', 'category-row');
    newRow.innerHTML = `<input type="text" class="search-input-outline search-category-input" placeholder="Category">`;

    if (afterRow) {
        afterRow.after(newRow);
    } else {
        const simpleForm = document.getElementById('simple-search-form');
        simpleForm.parentNode.insertBefore(newRow, simpleForm.nextSibling);
    }

    const newInput = newRow.querySelector('input');
    setupCategoryInputEvents(newInput);
}


// Function to add new tag+description row
function addTagRow() {
    const newRow = document.createElement('div');
    newRow.classList.add('search-input-row');
    newRow.innerHTML = `
        <input type="text" class="search-input-outline search-tag-input" placeholder="Tag">
        <input type="text" class="search-input-outline search-description-input" placeholder="Description">
    `;
    tagsContainer.appendChild(newRow);
    
    // Add event listeners to both inputs
    const tagInput = newRow.querySelector('.search-tag-input');
    const descInput = newRow.querySelector('.search-description-input');
    setupTagInputEvents(tagInput, descInput);
}

// Function to setup event listeners for category inputs
function setupCategoryInputEvents(input) {
    input.addEventListener('input', function() {
        // Check if we need to add a new empty input
        const allCategoryInputs = searchContainer.querySelectorAll('.search-input-row.category-row .search-category-input');
        const hasEmptyInput = Array.from(allCategoryInputs).some(inp => inp.value.trim() === '');
        
        if (!hasEmptyInput && this.value.trim() !== '') {
            addCategoryInputRow(this.closest('.search-input-row.category-row'));
        }
    });
    
    input.addEventListener('blur', function() {
        if (this.value.trim() === '') {
            const allCategoryInputs = searchContainer.querySelectorAll('.search-input-row.category-row .search-category-input');
            if (allCategoryInputs.length > 1) {
                this.closest('.search-input-row').remove();
            }
        }
    });
    
    input.addEventListener('focus', function() {
        this.classList.add('typing');
    });
    
    input.addEventListener('blur', function() {
        this.classList.remove('typing');
    });
}

function setupTagInputEvents(tagInput, descInput) {
    const handleInput = function() {
        const currentRow = this.closest('.search-input-row');
        const tagVal = currentRow.querySelector('.search-tag-input').value.trim();
        const descVal = currentRow.querySelector('.search-description-input').value.trim();
        
        if (tagVal !== '' && descVal !== '') {
            const allRows = tagsContainer.querySelectorAll('.search-input-row');
            const hasEmptyRow = Array.from(allRows).some(row => {
                const tag = row.querySelector('.search-tag-input').value.trim();
                const desc = row.querySelector('.search-description-input').value.trim();
                return tag === '' && desc === '';
            });
            
            if (!hasEmptyRow) {
                addTagRow();
            }
        }
    };
    
    const handleBlur = function() {
        const currentRow = this.closest('.search-input-row');
        const tagVal = currentRow.querySelector('.search-tag-input').value.trim();
        const descVal = currentRow.querySelector('.search-description-input').value.trim();
        
        if (tagVal === '' && descVal === '') {
            const allRows = tagsContainer.querySelectorAll('.search-input-row');
            const isFirstRow = currentRow === allRows[0];
            
            if (!isFirstRow) {
                currentRow.remove();
            }
        }
    };
    
    const handleFocus = function() {
        this.classList.add('typing');
    };
    
    const handleBlurStyling = function() {
        this.classList.remove('typing');
    };
    
    tagInput.addEventListener('input', handleInput);
    descInput.addEventListener('input', handleInput);
    tagInput.addEventListener('blur', handleBlur);
    descInput.addEventListener('blur', handleBlur);
    tagInput.addEventListener('focus', handleFocus);
    descInput.addEventListener('focus', handleFocus);
    tagInput.addEventListener('blur', handleBlurStyling);
    descInput.addEventListener('blur', handleBlurStyling);
}

function displayPins(pins) {
    var latlngs = [];
    pins.forEach((pin, idx) => {
            const marker = L.marker([pin.location.x, pin.location.y], { icon: getIconForCategory(pin.category) }).addTo(map);
            const tagsAndDescriptions = [
                { tag: 'Category', description: pin.category },
                ...Object.entries(pin.tags || {}).map(([tag, description]) => ({tag, description}))
            ];
            latlngs.push(marker.getLatLng());
            const popupContent = buildPopUpContent(tagsAndDescriptions, idx);
            marker.bindPopup(popupContent);
            placedPins.push({ pin: marker, tagsAndDescriptions: tagsAndDescriptions, id: pin.id });
            setupPopupButtonEvents(marker, idx);
        });
    return latlngs;
}


async function advancedSearch() {
    const params = getSearchParameters();
    try {
        if (params.toString() === '')
            loadPins();
        const response = await fetch(`/pins/search?${params.toString()}`);
        const pins = await response.json();
        placedPins.forEach(info => map.removeLayer(info.pin));
        placedPins.length = 0;
        if (pins && pins.length > 0) {
            var latlngs = displayPins(pins); 
            var bounds = new L.LatLngBounds(latlngs);
            map.fitBounds(bounds);
        }
    } catch(err) {
        console.error("Error searching:", err);
    }
}

searchCancelButton.addEventListener('click', closeSearchForm);           //  Listener for the Cancel button that closes the Advanced Search Form
searchSubmitButton.addEventListener('click', handleSearchSubmit); //  Listener for the Advanced Search button (Search button)

// Initialize dynamic input behavior
function initializeDynamicInputs() {
    // Setup event listeners for existing tag/description inputs
    const tagInput = tagsContainer.querySelector('.search-tag-input');
    const descriptionInput = tagsContainer.querySelector('.search-description-input');
    
    // Setup event listeners for the first Tag and Description inputs
    if (tagInput && descriptionInput) {
        setupTagInputEvents(tagInput, descriptionInput);
    }
    
    const mainSearchInput = document.querySelector('.simple-search-text');
    
    if (mainSearchInput) {
        mainSearchInput.addEventListener('input', function() {
            const isAdvancedActive = searchContainer.classList.contains('advanced-active');
            
            if (this.value.trim() !== '' && isAdvancedActive) {
                const existingCategoryInputs = searchContainer.querySelectorAll('.search-input-row.category-row .search-category-input');
                if (existingCategoryInputs.length === 0) {
                    addCategoryInputRow();
                }
            } else if (this.value.trim() === '' && isAdvancedActive) {
                const categoryRows = searchContainer.querySelectorAll('.search-input-row.category-row');
                categoryRows.forEach(row => {
                    const input = row.querySelector('.search-category-input');
                    if (input && input.value.trim() === '') {
                        row.remove();
                    }
                });
            }
        });
        
        mainSearchInput.addEventListener('focus', function() {
            this.classList.add('typing');
        });
        
        mainSearchInput.addEventListener('blur', function() {
            this.classList.remove('typing');
        });
    }
    
    const bboxInput = document.getElementById('search-bbox-input');
    const afterInput = document.getElementById('search-after-input');
    
    if (bboxInput) {
        bboxInput.addEventListener('focus', function() {
            this.classList.add('typing');
        });
        
        bboxInput.addEventListener('blur', function() {
            this.classList.remove('typing');
        });
    }
    
    if (afterInput) {
        afterInput.addEventListener('focus', function() {
            this.classList.add('typing');
        });
        
        afterInput.addEventListener('blur', function() {
            this.classList.remove('typing');
        });
    }
    
    const emailInput = document.querySelector('.search-input-outline[placeholder="Enter your email"]');
    
    if (emailInput) {
        emailInput.addEventListener('focus', function() {
            this.classList.add('typing');
        });
        
        emailInput.addEventListener('blur', function() {
            this.classList.remove('typing');
        });
    }
}
function getSearchParameters() {
    const params = new URLSearchParams();
    const categories = [];
    const after = document.getElementById('search-after-input').value;
    var bbox = document.getElementById('search-bbox-input').value.trim();
    
    const mainSearchValue = document.querySelector('.simple-search-text').value.trim();
    if (mainSearchValue !== '') {
        categories.push(mainSearchValue);
    }
    
    const categoryInputs = searchContainer.querySelectorAll('.search-category-input');
    categoryInputs.forEach(input => {
        const value = input.value.trim();
        if (value !== '') {
            categories.push(value);
        }
    });
    
    const tags = {};
    const tagRows = tagsContainer.querySelectorAll('.search-input-row');
    
    tagRows.forEach((row, index) => {
        const tagInput = row.querySelector('.search-tag-input');
        const descInput = row.querySelector('.search-description-input');
        const tagValue = tagInput ? tagInput.value.trim() : '';
        const descValue = descInput ? descInput.value.trim() : '';
        
        if (tagValue !== '' && descValue !== '') {
            tags[tagValue] = descValue;
        }
    });
    
    const filteredTags = Object.fromEntries(Object.entries(tags).filter(([key, value]) => key.trim() !== "" && value.trim() !== ""));
    
    if (categories && categories.length > 0) {
        params.append("categories", categories);
    }
    if (!bbox || bbox === "") {
        bbox = getCurrentBounds();
        // bbox = document.getElementById('default-bbox-input').value.trim();
    }
    if (bbox) params.append("bbox", bbox);
    if (after) params.append("after", after);
    if (filteredTags && Object.keys(filteredTags).length > 0) {
        for (const [key, value] of Object.entries(filteredTags)) {
            params.append(`tags[${key}]`, value);
        }
    }
    return params;
}
async function subscribe() {
    var params = getSearchParameters();
    var tz_offset = new Date().getTimezoneOffset();
    // params.append("tz_offset", tz_offset)
    
    const email = document.getElementById('search-email-input').value.trim();
    const validateEmail = (email) => {
        return String(email)
            .toLowerCase()
            .match(
            /^(([^<>()[\]\\.,;:\s@"]+(\.[^<>()[\]\\.,;:\s@"]+)*)|.(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/
            );
        };
    if (validateEmail(email)) {
        document.getElementById('error-msg').style.display = 'none';
        const requestBody = { email: email, tz_offset: tz_offset};
        try {
            if (params.toString() !== '') {
                const response = await fetch(`/subscribe?${params.toString()}`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify(requestBody)
                });
            }   
            
        } catch(err) {
            console.error("Error searching:", err);
        }
    } else {
        document.getElementById('error-msg').style.display = 'block';

    }
    
}

document.addEventListener('DOMContentLoaded', initializeDynamicInputs);

document.addEventListener('DOMContentLoaded', function() {
    const advancedButton = document.querySelector('.advanced-search-button');
    if (advancedButton) {
        advancedButton.addEventListener('click', openSearchForm);   // Advanced Search button (Advanced text button)
    }
});

document.querySelector(".simple-search-svg").addEventListener("click", () => { advancedSearch() }); // Listener for the Simple Search button (Magnifier Icon)
document.querySelector(".email-button-outline").addEventListener("click", () =>{ subscribe() });     // Listener for the Email button that sends an email to the user

const  followText = document.querySelector(".subscribe");  // Subscribe text button
followText.addEventListener("click", () =>{
    const searchInputOutline = document.querySelector(".search-input-outline[placeholder='Enter your email']");  // Search input field for the email
    const emailButtonOutline = document.querySelector(".email-button-outline");                                                                     // Email button that sends an email to the user
    searchInputOutline.classList.toggle("hidden");
    emailButtonOutline.classList.toggle("hidden");
});