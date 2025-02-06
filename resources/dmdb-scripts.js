document.addEventListener("click", (event) => {
	const infobox = document.getElementById("civ-search-info");
	if(infobox.style.display == "block" && !infobox.contains(event.target)) {
		infobox.style.display = "none";
	}
});

document.getElementById("filter-form").addEventListener("submit", function (event) {
    const form = event.target;
    const selectElements = form.querySelectorAll("select");
    const searchByName = document.getElementById("search_term");
    selectElements.forEach((select) => {
        if(select.value.length===0) {
            select.removeAttribute("name");
        }
    });
    if(searchByName.value.length===0) {
        searchByName.removeAttribute("name");
    }
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
            break;
        case 'rarity':
            columnIndex = 7;
    }
    
    const resultsTable = document.getElementById("results-table");
    const resultsBody = resultsTable.querySelector('tbody');
    const rows = Array.from(resultsBody.getElementsByTagName('tr'));
    
    if(sortParameter=='set') {
        function setCollNumSort(row) {
            const iconSrc = row.querySelector('img').getAttribute('src');
            const splitSrc = iconSrc.split("/")[1];
            const setName = splitSrc.split(".")[0];
            const splitSetName = setName.split("-");
            let setNumber = 1;
            let sortVal = 0;
            if(setName=='promo') sortVal += 9000;
            else if(splitSetName.length===2) {
                setNumber = parseInt(splitSetName[1]);
            }
            let collNum = row.getElementsByTagName('td')[8].textContent;
            for(let i = 0; i<collNum.length; i++) {
                if(collNum.charAt(i)=='/') {
                    if(collNum.charAt(0)=='S') {
                        sortVal += parseInt(collNum.substring(1,i));
                    }
                    else if(i+2<collNum.length&&collNum.charAt(i+1)=='Y') {
                        sortVal += parseInt(collNum.substring(1,i));
                        if(collNum.charAt(0)=='L'&&collNum.charAt(collNum.length-1)=='2') {
                            sortVal += 20;
                        }
                        else if(collNum.charAt(0)=='M') sortVal += 50;
                        else if(collNum.charAt(0)=='P') sortVal += 100;
                    }
                    else {
                        const val = parseInt(collNum.substring(0,i));
                        sortVal += (val+10);
                    }
                }
            }
            sortVal += ((setNumber-1)*120);
            return sortVal;
        }
        rows.sort((a, b) => setCollNumSort(a) - setCollNumSort(b));
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
            if(sortParameter=='rarity') {
                function mapRarity(rarity) {
                    const rarityMapping = {
                        'NR': 0,
                        'C': 1,
                        'U': 2,
                        'R': 3,
                        'VR': 4,
                        'SR': 5
                    };
                    return rarityMapping[rarity];
                }
                const aNumer = mapRarity(aValue);
                const bNumer = mapRarity(bValue);
                return bNumer - aNumer;
            }
            else return aValue.localeCompare(bValue);
        });
    }
    
    resultsBody.innerHTML = "";
    let cardNum = 0;
    
    rows.forEach(row => {
        cardNum++;
        if(cardNum%2==1) {
            row.style.backgroundColor = "#ffffff";
            row.onmouseout = function() {
                this.style.backgroundColor = "#ffffff";
            }
        }
        else {
            row.style.backgroundColor = "#eaeaea";
            row.onmouseout = function() {
                this.style.backgroundColor = "#eaeaea";
            }
        }
        row.getElementsByTagName('td')[0].textContent = cardNum + '.';
        resultsBody.appendChild(row);
    });
}   