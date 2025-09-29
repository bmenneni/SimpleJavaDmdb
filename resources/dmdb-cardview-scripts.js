const mediaContainer = document.getElementById("media-container");
const cardVideoContainer = document.getElementById("video-container");
const cardVideo = document.querySelector('video');
const cardImage = document.querySelector('#media-container img');
const toggleBtn = document.getElementById("toggle-btn");

function showVideo() {
	cardImage.style.display = "none";
	cardVideoContainer.style.display = "block";
	cardVideo.currentTime = 0;
	cardVideo.play();
	toggleBtn.textContent = "Stop video";
}

function showImage() {
	cardVideo.pause();
	cardVideoContainer.style.display = "none";
	cardImage.style.display = "block";
	toggleBtn.textContent = "Play video";
}

if(toggleBtn) {
	toggleBtn.addEventListener('click', () => {
		if(cardVideoContainer.style.display !== "none") {
			showImage();
		} else {
			showVideo();
		}
	});
}

if(cardVideo) {
	cardVideo.addEventListener('loadedmetadata', () => {
		if(cardVideo.duration >= 8) {
			cardVideo.loop = true;
		}
	});
	cardVideo.addEventListener('ended', () => {
		setTimeout(() => {
			if(!cardVideo.loop) {
				showImage();
			}
		}, 250);
	});
}