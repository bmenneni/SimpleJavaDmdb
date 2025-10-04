const baseUrl = window.location.origin;
const cardInfobox = document.getElementById("civ-search-info");
const searchInfobox = document.getElementById("search-term-info");
const resultsTable = document.getElementById("results-table");

document.addEventListener("click", (event) => {
	if(cardInfobox.style.display == 'block' && !cardInfobox.contains(event.target)) {
		cardInfobox.style.display = 'none';
	}
    if(searchInfobox.style.display == 'block' && !searchInfobox.contains(event.target)) {
        searchInfobox.style.display = 'none';
    }
});

document.getElementById("filter-form").addEventListener("submit", function (event) {
    const form = event.target;
    const selectElements = form.querySelectorAll("select");
    const civFilter = document.getElementById("civ_filter");
    const civMatch = document.querySelectorAll(".match_filter");
    const searchByName = document.getElementById("search-term");
    selectElements.forEach((select) => {
        if(select.value.length===0) {
            select.removeAttribute("name");
        }
    });
    if(civFilter.value.length===0) {
        civMatch.forEach((element) => {
            element.removeAttribute("name");
        });
    }
    if(searchByName.value.length===0) {
        searchByName.removeAttribute("name");
    }
});

if(resultsTable) {
    resultsTable.querySelectorAll("tbody tr").forEach(row => {
        const card_id = row.dataset.id;
        const card_image = document.getElementById("card-image");
        const image_panel = card_image.parentElement;
        const imgURL = "https://img.duelmasters.us/" + card_id + ".webp";
        row.addEventListener("mouseenter", function() {
            card_image.src = imgURL;
            image_panel.style.display = "inline-block";
        });
        row.addEventListener("mouseleave", function() {
            image_panel.style.display = "none";
        });
        row.addEventListener("click", function(event) {
            const cardviewUrl = new URL("/cardview/" + card_id, baseUrl);
            window.open(cardviewUrl, '_blank');
        });
    });
}

function toggleCardInfoBox(event) {
    event.stopPropagation();
    cardInfobox.style.display = "block";
}

function toggleSearchTermInfoBox(event) {
    event.stopPropagation();
    searchInfobox.style.display = "block";
}

function resetPage() {
	window.location = baseUrl;
}

function sort() {
    const sortParameter = document.getElementById("sort_by").value;
    const sortMode = document.getElementById("sort_mode").value;
    let columnIndex;
    switch(sortParameter) {
        case 'card_name':
            columnIndex = 1;
            break;
        case 'civilization':
            columnIndex = 2;
            break;
        case 'cost':
            columnIndex = 3;
            break;
        case 'card_type':
            columnIndex = 4;
            break;
        case 'race':
            columnIndex = 5;
            break;
        case 'power':
            columnIndex = 6;
            break;
        case 'rarity':
            columnIndex = 7;
    }
    
    const resultsBody = resultsTable.querySelector('tbody');
    const rows = Array.from(resultsBody.getElementsByTagName('tr'));

    if(sortParameter=='') {
        rows.sort((a, b) => {
            if(sortMode=='desc') {
                return b.dataset.id - a.dataset.id;
            } else {
                return a.dataset.id - b.dataset.id;               
            }
        });
    }

    else if(sortParameter=='civilization') {
        rows.sort((a, b) => {
            let aValue = a.getElementsByTagName('td')[columnIndex].querySelector('img').getAttribute('title');
            let bValue = b.getElementsByTagName('td')[columnIndex].querySelector('img').getAttribute('title');
            if(sortMode=='desc') {
                return bValue.localeCompare(aValue);              
            } else {
                return aValue.localeCompare(bValue);
            }
        });
    }

    else if(sortParameter=='rarity') {
        function getRarityValue(row) {
            const rarityElement = row.getElementsByTagName('td')[columnIndex].querySelector('img');
            let rarityValue = 0;
            let rarity = "";
            if(rarityElement) {
                rarity = rarityElement.getAttribute('title');
            }
            switch(rarity) {
                case "Common":
                    rarityValue = 1;
                    break;
                case "Uncommon":
                    rarityValue = 2;
                    break;
                case "Rare":
                    rarityValue = 3;
                    break;
                case "Very Rare":
                    rarityValue = 4;
                    break;
                case "Super Rare":
                    rarityValue = 5;
            }
            return rarityValue;
        }
        if(sortMode=='desc') {
            rows.sort((a, b) => getRarityValue(b) - getRarityValue(a));
        } else {
            rows.sort((a, b) => getRarityValue(a) - getRarityValue(b));
        }
    }

    else if(sortParameter=='cost') {
        rows.sort((a, b) => {
            let aInt = parseInt(a.getElementsByTagName('td')[columnIndex].textContent);
            let bInt = parseInt(b.getElementsByTagName('td')[columnIndex].textContent);
            if(sortMode=='desc') {
                return bInt - aInt;
            } else return aInt - bInt;
        });
    }

    else if(sortParameter=='power') {
        rows.sort((a, b) => {
            let aInt;
            let bInt;
            if((a.getElementsByTagName('td')[columnIndex].textContent)==="") {
                aInt = -100;
            } else {
                aInt = parseInt(a.getElementsByTagName('td')[columnIndex].textContent);
            }
            if((b.getElementsByTagName('td')[columnIndex].textContent)==="") {
                bInt = -100;
            } else {
                bInt = parseInt(b.getElementsByTagName('td')[columnIndex].textContent);
            }
            if(sortMode=='desc') {
                return bInt - aInt;
            } else return aInt - bInt;
        });
    }

    else {
        rows.sort((a, b) => {
            let aValue = a.getElementsByTagName('td')[columnIndex].textContent;
            let bValue = b.getElementsByTagName('td')[columnIndex].textContent;
            if(sortMode=='desc') {
                return bValue.localeCompare(aValue);              
            } else {
                return aValue.localeCompare(bValue);
            }
        });
    }
    
    resultsBody.innerHTML = "";
    let cardNum = 0;
    
    rows.forEach(row => {
        cardNum++;
        row.getElementsByTagName('td')[0].textContent = cardNum + '.';
        resultsBody.appendChild(row);
    });
}