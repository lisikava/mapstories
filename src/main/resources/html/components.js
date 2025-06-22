window.onload = async function () {
    await loadPins();
}

var map = L.map('map', {
    center: [50.065870, 19.934727],
    zoom: 12,
    zoomControl: false,
    attributionControl: false
});

L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
    maxZoom: 19,
}).addTo(map);

const pinIcon = L.icon({
    iconUrl: './assets/pin-3.svg',
    iconSize: [40, 40],
});

const reportIcon = L.icon({
    iconUrl: './assets/incident-1.svg',
    iconSize: [40, 40],
});

function getCSSVariable(variableName) {
    return getComputedStyle(document.documentElement).getPropertyValue(variableName).trim();
}

const eventCSSVariables = {
    'event.restaurant': '--event-restaurant',
    'story': '--story',
    'event.bar': '--event-bar',
    'event.park': '--event-park',
    'event.museum': '--event-museum',
    'event': '--event',
    'found': '--found',
    'lost': '--lost',
    'default': '--default'
};

function createColoredPinIcon(color) {
    const svgContent = `
        <svg width="800px" height="800px" viewBox="0 0 48 48" xmlns="http://www.w3.org/2000/svg" fill="${color}">
            <g id="SVGRepo_bgCarrier" stroke-width="0"/>
            <g id="SVGRepo_tracerCarrier" stroke-linecap="round" stroke-linejoin="round"/>
            <g id="SVGRepo_iconCarrier">
                <g id="Layer_2" data-name="Layer 2">
                    <g id="invisible_box" data-name="invisible box">
                        <rect width="48" height="48" fill="none"/>
                    </g>
                    <g id="icons_Q2" data-name="icons Q2">
                        <path d="M24,4a12,12,0,0,0-2,23.8V42a2,2,0,0,0,4,0V27.8A12,12,0,0,0,24,4Zm0,16a4,4,0,1,1,4-4A4,4,0,0,1,24,20Z"/>
                    </g>
                </g>
            </g>
        </svg>
    `;
    
    const blob = new Blob([svgContent], { type: 'image/svg+xml' });
    const url = URL.createObjectURL(blob);
    
    return L.icon({
        iconUrl: url,
        iconSize: [40, 40],
        iconAnchor: [20, 40],
        popupAnchor: [0, -40]
    });
}

function getIconForCategory(category) {
    if (!category) return pinIcon;
    
    const categoryLower = category.toLowerCase();
    
    if (categoryLower.includes('report')) {
        return reportIcon;
    }
    
    // if (categoryLower.startsWith('event.')) {
        const eventType = categoryLower;
        const cssVariableName = eventCSSVariables[eventType];
        
        if (cssVariableName) {
            const color = getCSSVariable(cssVariableName);
            return createColoredPinIcon(color);
        } else {
            const defaultColor = getCSSVariable(eventCSSVariables['default']);
            return createColoredPinIcon(defaultColor);
        }
    // }
    
    return pinIcon;
}

const style = document.createElement('style');
style.textContent = `
    .description-input-group input.error {
        border: 2px solid #ff4444;
        background-color: rgba(255, 0, 0, 0.05);
    }
`;
document.head.appendChild(style);

const pinsDescriptionContainer = document.getElementById('pins-description-container');
const descriptionContainer = document.getElementById('description-container');
const addCellButton = document.getElementById('add-cell');
const submitDescriptionButton = document.getElementById('submit-description');
const cancelDescriptionButton = document.getElementById('cancel-description');
const searchButton = document.getElementById('lasso-wrap');
const advancedSearchFormContainer = document.getElementById('advanced-search-form-container');
const searchCancelButton = document.getElementById('search-cancel');
const searchSubmitButton = document.getElementById('search-submit');
const addTagRowButton = document.getElementById('add-tag-row');
const removeTagRowButton = document.getElementById('remove-tag-row');
const tagsContainer = document.getElementById('tags-container');

let currentPin;
let createFormOpen = false;
let editFormOpen = false;
let searchFormOpen = false;
const allDescriptions = [];
const placedPins = [];

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
    pinsDescriptionContainer.classList.remove('hidden');
    createFormOpen = true;
    currentPin = pinInfo.pin;

    descriptionContainer.innerHTML = '';
    if (tagsAndDescriptions && tagsAndDescriptions.length > 0) {
        tagsAndDescriptions.forEach(pair => {
            const descriptionDiv = document.createElement('div');
            descriptionDiv.classList.add('description-input-group');
            descriptionDiv.classList.add('description-input-group');
            descriptionDiv.innerHTML = `
                <input type="text" class="tag-input" value="${pair.tag}" ${pair.tag === "Category" ? "disabled" : ""}>
                <input type="text" class="description-input" value="${pair.description}">
            `;
            descriptionContainer.appendChild(descriptionDiv);
        });
    }
    addListeners();
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
        pinsDescriptionContainer.classList.remove('hidden');
        descriptionContainer.innerHTML = `
                    <div class="description-input-group category-input-group">
<!--                        <div class="category-label"><b>Category</b></div>-->
                        <input type="text" class="tag-input" disabled value="Category">
                        <input type="text" class="description-input" placeholder="Enter category">
                   </div>
                `;

        descriptionContainer.scrollTop = 0;
        currentPin = L.marker(e.latlng, { icon: pinIcon }).addTo(map);
        addListeners();
    } else if (!pinClicked && createFormOpen) {
        pinsDescriptionContainer.classList.add('hidden');
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
        const response = await fetch('/pins');
        const pins = await response.json();
        displayPins(pins);
        map.setView([50.065870, 19.934727], 12);
    } catch (error) {
        console.error('Failed to load pins:', error);
    }
}

// Function that opens the Advanced Search Form
function openSearchForm() {
    const searchButtonsContainer = document.querySelector('.search-buttons-container');
    
    if (searchFormOpen) {
        advancedSearchFormContainer.classList.add('hidden');
        searchButtonsContainer.classList.remove('active');
        searchFormOpen = false;
    }   // If the Advanced Search Form is already open, close Pins Description Container
    else {
        if (createFormOpen) {
            pinsDescriptionContainer.classList.add('hidden');
            createFormOpen = false;
            if (currentPin && !editFormOpen) {
                map.removeLayer(currentPin);
                currentPin = null;
            }
        }
        advancedSearchFormContainer.classList.remove('hidden');
        searchButtonsContainer.classList.add('active');
        searchFormOpen = true;
    }
}

// Function that closes the Advanced Search Form 
function closeSearchForm() {
    const searchButtonsContainer = document.querySelector('.search-buttons-container');
    
    advancedSearchFormContainer.classList.add('hidden');
    searchButtonsContainer.classList.remove('active');
    searchFormOpen = false;
}

// Function that handles the search submit
function handleSearchSubmit() {

    closeSearchForm();
}

// Function to add new tag + description row
function addTagRow() {
    const newTagRow = document.createElement('div');
    newTagRow.classList.add('search-input-row');
    newTagRow.innerHTML = `
        <input type="text" class="search-input-outline" placeholder="Tag">
        <input type="text" class="search-input-outline" placeholder="Description">
    `;
    tagsContainer.appendChild(newTagRow);
}

function removeTagRow() {
    const tagRows = tagsContainer.querySelectorAll('.search-input-row');
    if (tagRows.length > 1) {
        const lastRow = tagRows[tagRows.length - 1];
        tagsContainer.removeChild(lastRow);
    }
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

async function simpleSearch(category) {
    try {
        if (category === '')
            loadPins();
        const response = await fetch(`/pins/search?category=${encodeURIComponent(category)}`);
        const pins = await response.json();
        // if (!pins.length) {
        //     loadPins();
        // }
        placedPins.forEach(info => map.removeLayer(info.pin));
        placedPins.length = 0;
        var latlngs = displayPins(pins); 
        var bounds = new L.LatLngBounds(latlngs);
        map.fitBounds(bounds);
    } catch(err) {
        console.error("Error searching:", err);
    }
}

searchCancelButton.addEventListener('click', closeSearchForm);           //  Listener for the Cancel button that closes the Advanced Search Form
searchSubmitButton.addEventListener('click', handleSearchSubmit); //  Listener for the Advanced Search button (Search button)
addTagRowButton.addEventListener('click', addTagRow);             //  Listener for the Add Tag Row button
removeTagRowButton.addEventListener('click', removeTagRow);       //  Listener for the Remove Tag Row button

document.addEventListener('DOMContentLoaded', function() {
    const advancedButton = document.querySelector('.advanced-search-button');
    if (advancedButton) {
        advancedButton.addEventListener('click', openSearchForm);   // Advanced Search button (Advanced text button)
    }
});


document.querySelector(".simple-search-svg").addEventListener("click", () => { simpleSearch(document.querySelector(".simple-search-text").value.trim()) }); // Listener for the Simple Search button (Magnifier Icon)
document.querySelector(".email-button-outline").addEventListener("click", () =>{console.log("Email sent")});     // Listener for the Email button that sends an email to the user


const  followText = document.querySelector(".subscribe");  // Subscribe text button
followText.addEventListener("click", () =>{
    const searchInputOutline = document.querySelector(".search-input-outline[placeholder='Enter your email']");  // Search input field for the email
    const emailButtonOutline = document.querySelector(".email-button-outline");                                                                     // Email button that sends an email to the user
    searchInputOutline.classList.toggle("hidden");
    emailButtonOutline.classList.toggle("hidden");
});