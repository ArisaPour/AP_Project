document.addEventListener("DOMContentLoaded", function () {
    document.getElementById("recommendButton").addEventListener("click", function () {
        console.log("Button clicked! Fetching recommendations...");

        const movieName = document.getElementById("movieName").value.trim();
        const movieGenre = document.getElementById("movieGenre").value.trim();
        const count = document.getElementById("count").value.trim() || 5;
        const resultsDiv = document.getElementById("results");
        const loadingDiv = document.getElementById("loading");
        const playGameBtn = document.getElementById("playGame");

        if (!movieName || !movieGenre) {
            alert("Please enter both movie name and genre!");
            return;
        }

        // Show Loading Message & Play Game Button
        resultsDiv.innerHTML = "";
        loadingDiv.classList.remove("hidden");
        playGameBtn.classList.remove("hidden");

        // Play Game Button Click → Opens CrazyGames
        playGameBtn.addEventListener("click", function () {
            window.open("https://www.crazygames.com/", "_blank");
        });

        const apiUrl = `http://localhost:8080/api/recommend?name=${encodeURIComponent(movieName)}&genre=${encodeURIComponent(movieGenre)}&count=${count}`;
        console.log("Requesting API:", apiUrl);

        fetch(apiUrl)
            .then(response => {
                console.log("Response received:", response);
                if (!response.ok) {
                    return response.text().then(text => { throw new Error(text); });
                }
                return response.json();
            })
            .then(data => {
                console.log("Received data:", data);
                loadingDiv.classList.add("hidden"); // Hide loading message
                playGameBtn.classList.add("hidden"); // Hide game button
                resultsDiv.innerHTML = ""; // Clear previous results

                if (data.length === 0) {
                    resultsDiv.innerHTML = `<p>No recommendations found.</p>`;
                    return;
                }

                let resultHTML = "<h2>Recommended Movies:</h2>";
                data.forEach(movie => {
                    resultHTML += `
                        <div class="movie-card">
                            <h2>${movie.name}</h2>
                            <p><strong>Rating:</strong> ${movie.rating}</p>
                            <p><strong>Description:</strong> ${movie.description}</p>
                            <p><strong>Director:</strong> ${movie.director}</p>
                            <p><strong>Actors:</strong> ${movie.actors}</p>
                            <p><strong>Similarity:</strong> ${movie.similarity}</p>
                        </div>
                    `;
                });

                resultsDiv.innerHTML = resultHTML;
            })
            .catch(error => {
                console.error("Error fetching recommendations:", error);
                loadingDiv.classList.add("hidden");
                playGameBtn.classList.add("hidden");
                resultsDiv.innerHTML = `<p class="error-text">Error fetching recommendations: ${error.message}</p>`;
            });
    });
});

