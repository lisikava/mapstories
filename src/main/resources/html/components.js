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

// Add CSS for error state
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

let currentPin;
let createFormOpen = false;
let editFormOpen = false;
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
    const descriptionGroups = descriptionContainer.querySelectorAll('.description-input-group');
    let hasEmptyFields = false;

    descriptionGroups.forEach(group => {
        const inputs = group.querySelectorAll('input');
        inputs.forEach(input => {
            input.classList.remove('error');
        });
    });

    descriptionGroups.forEach(group => {
        const tagInput = group.querySelector('.tag-input');
        const descriptionInput = group.querySelector('.description-input');
        
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

    let popupContent = buildPopUpContent(currentPinTagsAndDescriptions, placedPins.indexOf(currentPin));
    currentPin.bindPopup(popupContent).openPopup();

    const categoryObj = currentPinTagsAndDescriptions.find(pair => pair.tag === "Category");
    const category = categoryObj ? categoryObj.description : "";
    const tags = {};
    currentPinTagsAndDescriptions.forEach(pair => {
        if (pair.tag !== "Category") {
            tags[pair.tag] = pair.description;
        }
    });

    const pinData = { location: { lat: currentPin.getLatLng().lat, lon: currentPin.getLatLng().lng }, category, tags };
    const response = await fetch('/pins', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(pinData)
    }).catch(error => {
        console.error(error);
    });
    const result = await response.json();
    placedPins.push({ pin: currentPin, tagsAndDescriptions: currentPinTagsAndDescriptions });

    pinsDescriptionContainer.classList.add('hidden');
    currentPin = null;
    createFormOpen = false;
    removeListeners();
};

const cancelDescriptionHandler = function () {
    map.removeLayer(currentPin);
    pinsDescriptionContainer.classList.add('hidden');
    currentPin = null;
    createFormOpen = false;
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

function deletePin(pinIndex) {
    alert('Delete pin ' + pinIndex);
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
        pins.forEach((pin, idx) => {
            const marker = L.marker([pin.location.x, pin.location.y], { icon: pinIcon }).addTo(map);
            const tagsAndDescriptions = [
                { tag: 'Category', description: pin.category },
                ...Object.entries(pin.tags || {}).map(([tag, description]) => ({tag, description}))
            ];
            popupContent = buildPopUpContent(tagsAndDescriptions, idx);
            marker.bindPopup(popupContent);
            placedPins.push({ pin: marker, tagsAndDescriptions: tagsAndDescriptions });
            setupPopupButtonEvents(marker, idx);
        });
    } catch (error) {
        console.error('Failed to load pins:', error);
    }
}