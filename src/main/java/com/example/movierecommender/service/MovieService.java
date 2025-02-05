package com.example.movierecommender.service;

import com.example.movierecommender.util.CsvUtil;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MovieService {

    // Method to fetch movie recommendations based on the main movie, genre, and count
    public List<Map<String, String>> getRecommendedMovies(String movieName, String genre, int count) {
        // Define paths for CSV and embeddings files
        String csvFilePath = "..\\movie-recommeder\\src\\main\\java\\com\\example\\movierecommender\\database\\" + genre + ".csv";
        String embeddingsFilePath = "..\\movie-recommeder\\src\\main\\java\\com\\example\\movierecommender\\database\\" + genre + "_embeddings.csv";

        // Print movie and genre details
        System.out.println("Searching for movie: " + movieName + " in genre: " + genre);
        String mainMovieEmbedding = CsvUtil.extractMovieDetails(csvFilePath, movieName);

        // If the movie embedding is found, generate recommendations
        if (mainMovieEmbedding != null) {
            System.out.println("Generating recommendations...");

            // Check if the embeddings file exists
            File embeddingsFile = new File(embeddingsFilePath);
            Map<String, String> embeddingsMap;

            // If embeddings file exists, read from it; otherwise, generate new embeddings
            if (embeddingsFile.exists()) {
                System.out.println("Using existing embeddings file...");
                embeddingsMap = CsvUtil.readEmbeddings(embeddingsFilePath);
            } else {
                System.out.println("Generating new embeddings...");
                embeddingsMap = generateAndSaveEmbeddings(csvFilePath, embeddingsFilePath);
            }

            // Calculate the similarity between the main movie and others
            Map<String, Double> similarityMap = calculateSimilarities(embeddingsMap, mainMovieEmbedding, movieName);

            // Sort results and return a list of recommended movies
            return similarityMap.entrySet().stream()
                    .filter(entry -> !entry.getKey().equalsIgnoreCase(movieName)) // Exclude the main movie from results
                    .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue())) // Sort by similarity in descending order
                    .limit(count) // Limit to the specified count of recommendations
                    .map(entry -> {
                        // Get movie details and add similarity percentage to them
                        String recommendedMovieName = entry.getKey();
                        Map<String, String> movieDetails = CsvUtil.getMovieDetails(csvFilePath, recommendedMovieName);
                        movieDetails.put("similarity", String.format("%.2f", entry.getValue() * 100) + "%");
                        return movieDetails;
                    })
                    .collect(Collectors.toList()); // Collect and return the final list of recommended movies
        }

        // Return an empty list if no embedding was found for the movie
        return Collections.emptyList();
    }

    // Method to generate and save embeddings for movies
    private Map<String, String> generateAndSaveEmbeddings(String csvFilePath, String embeddingsFilePath) {
        Map<String, String> embeddingsMap = new HashMap<>();

        try (FileWriter writer = new FileWriter(embeddingsFilePath)) {
            // Read all movie data from CSV file
            List<String[]> allData = CsvUtil.readCsv(csvFilePath);
            writer.write("MovieName,Embedding\n"); // Write CSV header

            // Iterate over each movie entry and generate its embedding
            for (String[] row : allData) {
                if (row.length >= 10) {
                    String movieName = row[1];
                    String director = row[8];
                    String actors = row[10];

                    try {
                        // Generate embedding for the movie using the director and actors
                        String embedding = api.generateEmbedding(director, actors);
                        embeddingsMap.put(movieName, embedding);

                        // Write the movie name and its embedding to the CSV file (with | as separator for embedding values)
                        String embeddingWithPipe = embedding.replace(",", "|");
                        writer.write("\"" + movieName.replace("\"", "\"\"") + "\",\"" + embeddingWithPipe.replace("\n", "") + "\"\n");
                    } catch (Exception ignored) {} // Handle any exceptions silently
                }
            }
        } catch (IOException e) {
            e.printStackTrace(); // Print any I/O exceptions
        }

        return embeddingsMap; // Return the generated embeddings map
    }

    // Method to calculate cosine similarities between movie embeddings
    private Map<String, Double> calculateSimilarities(Map<String, String> embeddingsMap, String mainMovieEmbedding, String mainFilm) {
        Map<String, Double> similarityMap = new HashMap<>();

        // Iterate through the embeddings map and calculate similarity for each movie
        for (Map.Entry<String, String> entry : embeddingsMap.entrySet()) {
            if (!entry.getKey().equalsIgnoreCase(mainFilm)) {
                try {
                    // Calculate cosine similarity between the main movie and the current movie
                    double similarity = calculateCosineSimilarity(mainMovieEmbedding, entry.getValue());
                    similarityMap.put(entry.getKey(), similarity);
                } catch (Exception ignored) {} // Handle any exceptions silently
            }
        }

        return similarityMap; // Return the similarity map
    }

    // Method to calculate the cosine similarity between two vectors
    private double calculateCosineSimilarity(String vector1, String vector2) {
        // Parse the embedding vectors from JSON strings
        double[] vec1 = parseVector(vector1);
        double[] vec2 = parseVector(vector2);

        // Calculate the dot product and norms of the vectors
        double dotProduct = 0.0, normVec1 = 0.0, normVec2 = 0.0;
        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            normVec1 += Math.pow(vec1[i], 2);
            normVec2 += Math.pow(vec2[i], 2);
        }

        // Return cosine similarity
        return dotProduct / (Math.sqrt(normVec1) * Math.sqrt(normVec2));
    }

    // Method to parse the JSON string representing the embedding vector
    private double[] parseVector(String jsonString) {
        // Extract the values between square brackets in the JSON
        int startIndex = jsonString.indexOf("[");
        int endIndex = jsonString.indexOf("]");

        if (startIndex == -1 || endIndex == -1) {
            throw new IllegalArgumentException("Invalid JSON format");
        }

        // Split the values by comma and convert them into a double array
        String[] values = jsonString.substring(startIndex + 1, endIndex).split(",");
        return Arrays.stream(values).mapToDouble(Double::parseDouble).toArray();
    }
}

// API class for generating embeddings via an external API
class api {
    private static final String API_URL = "http://localhost:11434/api/embeddings";

    // Method to generate an embedding based on director and actors
    public static String generateEmbedding(String director, String actors) throws Exception {
        // Create the JSON payload for the request
        String jsonInputString = createJsonPayload(director, actors);
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonInputString))
                .build();

        // Send the request and return the response body (embedding)
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    // Method to create JSON payload for the API request
    private static String createJsonPayload(String director, String actors) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("model", "nomic-embed-text");
        jsonObject.put("prompt", director + " " + actors); // Use director and actors for generating the embedding
        return jsonObject.toString();
    }
}

