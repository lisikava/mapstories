window.onload = async function () {
    loadPins();
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

const pin_icon = L.icon({
    iconUrl: './assets/pin-3.svg',
    iconSize: [40, 40],
});

const pins_Description_Container = document.getElementById('pins-description-container');
const description_Container = document.getElementById('description-container');
const add_Cell_Button = document.getElementById('add-cell');
const submit_Description_Button = document.getElementById('submit-description');
const cancel_Description_Button = document.getElementById('cancel-description');

let current_pin;
let form_Open = false;
const all_Descriptions = [];
const placed_pins = [];

function remove_listeners() {
    add_Cell_Button.removeEventListener('click', add_Description_Handler);
    submit_Description_Button.removeEventListener('click', submit_Description_Handler);
    cancel_Description_Button.removeEventListener('click', cancel_Description_Handler);
}

function addListeners() {
    add_Cell_Button.addEventListener('click', add_Description_Handler);
    submit_Description_Button.addEventListener('click', submit_Description_Handler);
    cancel_Description_Button.addEventListener('click', cancel_Description_Handler);
}
const add_Description_Handler = function () {
    const new_Description_Input = document.createElement('div');
    new_Description_Input.classList.add('description-input-group');
    new_Description_Input.innerHTML = `
                        <input type="text" class="tag-input" placeholder="Tag">
                        <input type="text" class="description-input" placeholder="Enter description">
                `;
    description_Container.appendChild(new_Description_Input);
};

const submit_Description_Handler = function () {
    const description_Groups = description_Container.querySelectorAll('.description-input-group');
    const current_Pin_Tags_And_Descriptions = Array.from(description_Groups).map(pair => {
        const tag_Input = pair.querySelector('.tag-input');
        const description_Input = pair.querySelector('.description-input');
        const tag = tag_Input ? tag_Input.value.trim() : '';
        const description = description_Input ? description_Input.value.trim() : '';
        return { tag: tag, description: description };
    }).filter(pair => pair.tag != '' || pair.description !== '');

    if (current_Pin_Tags_And_Descriptions.length > 0) {
        let popup_Content = '';
        current_Pin_Tags_And_Descriptions.forEach(pair => {
            popup_Content += `<b>${pair.tag}:</b> ${pair.description}<br><hr>`;
        });
        current_pin.bindPopup(popup_Content).openPopup();
        placed_pins.push({ pin: current_pin, Tags_And_Descriptions: current_Pin_Tags_And_Descriptions });

    } else {
        map.removeLayer(current_pin);
    }
    pins_Description_Container.classList.add('hidden');
    current_pin = null;
    form_Open = false;
    remove_listeners();
};

const cancel_Description_Handler = function () {
    map.removeLayer(current_pin);
    pins_Description_Container.classList.add('hidden');
    current_pin = null;
    form_Open = false;
    remove_listeners();
};

function display_Pin_Content(tags_And_Descriptions) {
    description_Container.innerHTML = '';
    if (tags_And_Descriptions && tags_And_Descriptions.length > 0) {
        tags_And_Descriptions.forEach(desc => {
            const description_Div = document.createElement('div');
            description_Div.classList.add('description-input-group');
            description_Div.innerHTML = `<b>${pair.tag}:</b> ${pair.description}`;
            description_Container.appendChild(description_Div);
        });
        pins_Description_Container.classList.remove('hidden');
    } else {
        pins_Description_Container.classList.add('hidden');
    }
}

map.on('click', function (e) {
    let pin_Clicked = false;
    placed_pins.forEach(pinInfo => {
        if (e.layerPoint && pinInfo.pin.getLatLng().equals(e.latlng)) {
            display_Pin_Content(pinInfo.tags_And_Descriptions);
            pin_Clicked = true;
        }
    });

    if (!pin_Clicked && !form_Open) {
        form_Open = true;
        pins_Description_Container.classList.remove('hidden');
        description_Container.innerHTML = `
                    <div class="description-input-group category-input-group">
<!--                        <div class="category-label"><b>Category</b></div>-->
                        <input type="text" class="tag-input" disabled value="Category">
                        <input type="text" class="description-input" placeholder="Enter category">
                   </div>
                `;

        description_Container.scrollTop = 0;
        current_pin = L.marker(e.latlng, { icon: pin_icon }).addTo(map);
        addListeners();
    } else if (!pin_Clicked && form_Open) {
        pins_Description_Container.classList.add('hidden');
        form_Open = false;
        if (current_pin) {
            map.removeLayer(current_pin);
            current_pin = null;
            remove_listeners();
        }
    }

});

async function loadPins() {
    const response = await fetch('/pins');
    const pins = await response.json();
    pins.forEach(pin => {
        const marker = L.marker([pin.location.lat, pin.location.lon], { icon: pin_icon }).addTo(map);
        popup_Content = '';
        popup_Content += `<b>${'Category'}:</b> ${pin.category}<br><hr>`;
        current_pin = marker;
        current_pin.bindPopup(popup_Content);
        placed_pins.push({ pin: current_pin, Tags_And_Descriptions: { tag: 'Category', description: pin.category } });
    });
}
