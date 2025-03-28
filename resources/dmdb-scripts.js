document.addEventListener("click", (event) => {
	const infobox = document.getElementById("civ-search-info");
	if(infobox.style.display == "block" && !infobox.contains(event.target)) {
		infobox.style.display = "none";
	}
});

document.getElementById("filter-form").addEventListener("submit", function (event) {
    const form = event.target;
    const selectElements = form.querySelectorAll("select");
    const searchByName = document.getElementById("search-term");
    const sortParameter = document.getElementById("sort_by");
    const sortMode = document.getElementById("sort_mode");
    selectElements.forEach((select) => {
        if(select.value.length===0) {
            select.removeAttribute("name");
        }
    });
    if(searchByName.value.length===0) {
        searchByName.removeAttribute("name");
    }
    if(sortParameter.value=='set') {
        sortParameter.removeAttribute("name");
        if(sortMode.value=='asc') sortMode.removeAttribute("name");
    }
});

document.getElementById("results-table").querySelectorAll("tbody tr").forEach(row => {
    const card_id = row.dataset.id;
    const card_image = document.getElementById("card-image");
    const image_panel = card_image.parentElement;
    const imgURL = "https://d2d61uxafrrtdf.cloudfront.net/" + card_id + ".webp";
    row.addEventListener("mouseenter", function() {
        card_image.src = imgURL;
        image_panel.style.display = "inline-block";
    });
    row.addEventListener("mouseleave", function() {
        image_panel.style.display = "none";
    });
    row.addEventListener("click", function(event) {
        if(event.shiftKey) {
            window.open(imgURL, '_blank');
        }
        else {
            const card_name = row.getElementsByTagName('td')[1].textContent;
            const wikiURL = "https://duelmasters.fandom.com/wiki/" + card_name;
            window.open(wikiURL, '_blank');
        }
    });
});

function toggleInfoBox(event) {
    event.stopPropagation();
    const infobox = document.getElementById("civ-search-info");
    infobox.style.display = "block";
}

function resetPage() {
	window.location = window.location.origin;
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
    }
    
    const resultsTable = document.getElementById("results-table");
    const resultsBody = resultsTable.querySelector('tbody');
    const rows = Array.from(resultsBody.getElementsByTagName('tr'));

    if(sortParameter=='set') {
        rows.sort((a, b) => {
            if(sortMode=='desc') {
                return b.dataset.id - a.dataset.id;
            } else {
                return a.dataset.id - b.dataset.id;               
            }
        });
    }

    else if(sortParameter=='rarity') {
        function getRarityValue(row) {
            let rarity;
            let rarityValue;
            if(row.dataset.id>9005) {
                rarityValue = 0;
            } else {
                rarity = row.querySelector('img').title;
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
        if(sortMode=='asc') {
            rows.sort((a, b) => getRarityValue(a) - getRarityValue(b));
        } else {
            rows.sort((a, b) => getRarityValue(b) - getRarityValue(a));
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
            }
            else aInt = parseInt(a.getElementsByTagName('td')[columnIndex].textContent);
            if((b.getElementsByTagName('td')[columnIndex].textContent)==="") {
                bInt = -100;
            }
            else bInt = parseInt(b.getElementsByTagName('td')[columnIndex].textContent);
            if(sortMode=='asc') {
                return aInt - bInt;
            } else return bInt - aInt;
        });     
    }

    else {
        rows.sort((a, b) => {
            let aValue = a.getElementsByTagName('td')[columnIndex].textContent;
            let bValue = b.getElementsByTagName('td')[columnIndex].textContent;
            if(sortMode=='asc') {
                return aValue.localeCompare(bValue);              
            } else {
                return bValue.localeCompare(aValue);
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