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


const pinsDescriptionContainer = document.getElementById('pins-description-container');
const descriptionContainer = document.getElementById('description-container');
const addCellButton = document.getElementById('add-cell');
const submitDescriptionButton = document.getElementById('submit-description');
const cancelDescriptionButton = document.getElementById('cancel-description');

let currentPin;
let formOpen = false;
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

const submitDescriptionHandler = function () {
    const descriptionGroups = descriptionContainer.querySelectorAll('.description-input-group');
    const currentPinTagsAndDescriptions = Array.from(descriptionGroups).map(pair => {
        const tagInput = pair.querySelector('.tag-input');
        const descriptionInput = pair.querySelector('.description-input');
        const tag = tagInput ? tagInput.value.trim() : '';
        const description = descriptionInput ? descriptionInput.value.trim() : '';
        return { tag: tag, description: description };
    }).filter(pair => pair.tag !== '' || pair.description !== '');

    if (currentPinTagsAndDescriptions.length > 0) {
        let popupContent = buildPopUpContent(currentPinTagsAndDescriptions);
        currentPin.bindPopup(popupContent).openPopup();
        placedPins.push({ pin: currentPin, tagsAndDescriptions: currentPinTagsAndDescriptions });

    } else {
        map.removeLayer(currentPin);
    }
    pinsDescriptionContainer.classList.add('hidden');
    currentPin = null;
    formOpen = false;
    removeListeners();
};

const cancelDescriptionHandler = function () {
    map.removeLayer(currentPin);
    pinsDescriptionContainer.classList.add('hidden');
    currentPin = null;
    formOpen = false;
    removeListeners();
};

function buildPopUpContent(tagsAndDescriptions) {
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
    return container;
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

    if (!pinClicked && !formOpen) {
        formOpen = true;
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
    } else if (!pinClicked && formOpen) {
        pinsDescriptionContainer.classList.add('hidden');
        formOpen = false;
        if (currentPin) {
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
        pins.forEach(pin => {
            const marker = L.marker([pin.location.lat, pin.location.lon], { icon: pinIcon }).addTo(map);
            const tagsAndDescriptions = [
                { tag: 'Category', description: pin.category },
                ...Object.entries(pin.tags || {}).map(([tag, description]) => ({tag, description}))
            ];
            popupContent = buildPopUpContent(tagsAndDescriptions);
            // popupContent += `<b>${'Category'}:</b> ${pin.category}<br><hr>`;
            marker.bindPopup(popupContent);
            placedPins.push({ pin: marker, tagsAndDescriptions: tagsAndDescriptions });
        });
    } catch (error) {
        console.error('Failed to load pins:', error);
    }
}
