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
    selectElements.forEach((select) => {
        if(select.value.length===0) {
            select.removeAttribute("name");
        }
    });
    if(searchByName.value.length===0) {
        searchByName.removeAttribute("name");
    }
});

document.getElementById("results-table").querySelectorAll("tbody tr").forEach(row => {
    row.addEventListener("mouseenter", function() {
        let card_id = this.dataset.id;
        const card_image = document.getElementById("card-image");
        const image_panel = card_image.parentElement;
        card_image.src = "https://d2d61uxafrrtdf.cloudfront.net/" + card_id + ".webp";
        image_panel.style.display = "inline-block";
    });
    row.addEventListener("mouseleave", function() {
        document.getElementById("image-panel-div").style.display = "none";
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

function openCard(rowElement) {
    const card_name = rowElement.getElementsByTagName('td')[1].textContent;
    const wikiURL = "https://duelmasters.fandom.com/wiki/" + card_name;
    window.open(wikiURL, '_blank');
}

function sort() {
    const sortParameter = document.getElementById("sort_by").value;
    let columnIndex;
    switch(sortParameter) {
        case 'name':
            columnIndex = 1;
            break;
        case 'civilization':
            columnIndex = 2;
            break;
        case 'cost':
            columnIndex = 3;
            break;
        case 'type':
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
            return a.dataset.id - b.dataset.id;
        });
    }

    else if(sortParameter=='rarity') {
        function getRarityValue(row) {
            const rarity = row.querySelector('img').getAttribute('src').split("/")[1].split(".")[0].split("-")[1];
            let rarityValue;
            switch(rarity) {
                case 'nr' :
                    rarityValue = 0;
                    break;
                case 'c':
                    rarityValue = 1;
                    break;
                case 'u':
                    rarityValue = 2;
                    break;
                case 'r':
                    rarityValue = 3;
                    break;
                case 'vr':
                    rarityValue = 4;
                    break;
                case 'sr':
                    rarityValue = 5;
            }
            return rarityValue;
        }
        rows.sort((a, b) => getRarityValue(b) - getRarityValue(a));
    }

    else if(sortParameter=='cost') {
        rows.sort((a, b) => {
            let aInt = parseInt(a.getElementsByTagName('td')[columnIndex].textContent);
            let bInt = parseInt(b.getElementsByTagName('td')[columnIndex].textContent);
            return aInt - bInt;
        });
    }

    else if(sortParameter=='power') {
        rows.sort((a, b) => {
            let aInt;
            let bint;
            if((a.getElementsByTagName('td')[columnIndex].textContent)==="") {
                aInt = -100;
            }
            else aInt = parseInt(a.getElementsByTagName('td')[columnIndex].textContent);
            if((b.getElementsByTagName('td')[columnIndex].textContent)==="") {
                bInt = -100;
            }
            else bInt = parseInt(b.getElementsByTagName('td')[columnIndex].textContent);
            return bInt - aInt;
        });     
    }

    else {
        rows.sort((a, b) => {
            let aValue = a.getElementsByTagName('td')[columnIndex].textContent;
            let bValue = b.getElementsByTagName('td')[columnIndex].textContent;
            return aValue.localeCompare(bValue);
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