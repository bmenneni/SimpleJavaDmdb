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

function clearPage() {
	window.location = window.location.origin;
}

function openCard(rowElement) {
    var card_name = rowElement.getElementsByTagName('td')[1].textContent;
    var wikiURL = "https://duelmasters.fandom.com/wiki/" + card_name;
    window.open(wikiURL, '_blank');
}

function sort() {
    var sortParameter = document.getElementById("sort_by").value;
    var columnIndex;
    switch(sortParameter) {
        case 'set':
            columnIndex = 9;
            break;
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
    
    var resultsTable = document.getElementById("results-table");
    var resultsBody = resultsTable.querySelector('tbody');
    var rows = Array.from(resultsBody.getElementsByTagName('tr'));
    
    rows.sort((a, b) => {
        var aValue = a.getElementsByTagName('td')[columnIndex].textContent;
        var bValue = b.getElementsByTagName('td')[columnIndex].textContent;
        if(sortParameter=='rarity') {
            function mapRarity(rarity) {
                var rarityMapping = {
                    'C': 1,
                    'U': 2,
                    'R': 3,
                    'VR': 4,
                    'SR': 5
                };
                return rarityMapping[rarity];
            }
            var aNumer = mapRarity(aValue);
            var bNumer = mapRarity(bValue);
            return aNumer - bNumer;
        }
        else return aValue.localeCompare(bValue);
    });
    
    if(sortParameter=='set') {
        function collNumNormalize(row) {
            var setName = row.getElementsByTagName('td')[9].textContent;
            var setBonus;
            if(setName=='Promos') setBonus = 9000;
            else setBonus = (parseInt(setName.substring(setName.length-2))-1) * 120;
            var collNum = row.getElementsByTagName('td')[8].textContent;
            for(let i = 0; i<collNum.length; i++) {
                if(collNum.charAt(i)=='/'&&collNum.charAt(0)=='S') {
                    let val = parseInt(collNum.substring(1,i));
                    return val + setBonus;
                }
                else if(collNum.charAt(i)=='/') {
                    let val = parseInt(collNum.substring(0,i));
                    return val + setBonus;
                }
            }
        }
        rows.sort((a, b) => collNumNormalize(a) - collNumNormalize(b));
    }
    
    resultsBody.innerHTML = "";
    var cardNum = 0;
    
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