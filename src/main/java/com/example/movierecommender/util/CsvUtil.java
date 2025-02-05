package com.example.movierecommender.util;

import com.opencsv.*;
import org.json.JSONObject;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.util.*;

public class CsvUtil {

    /**
     * Reads the CSV file and returns a list of all rows (excluding the header).
     *
     * @param file The path to the CSV file.
     * @return A list of string arrays where each array represents a row in the CSV.
     */
    public static List<String[]> readCsv(String file) {
        try (FileReader filereader = new FileReader(file);
             CSVReader csvReader = new CSVReaderBuilder(filereader).withSkipLines(1).build()) {
            // Read all rows from the CSV
            return csvReader.readAll();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Retrieves the details of a specific movie from the CSV file.
     *
     * @param filePath   The path to the CSV file.
     * @param movieName  The name of the movie to fetch details for.
     * @return A map containing the movie details such as name, rating, description, director, and actors.
     */
    public static Map<String, String> getMovieDetails(String filePath, String movieName) {
        Map<String, String> movieDetails = new HashMap<>();

        try (FileReader filereader = new FileReader(filePath);
             CSVReader csvReader = new CSVReaderBuilder(filereader).withSkipLines(1).build()) {

            List<String[]> allData = csvReader.readAll();

            // Iterate through all rows to find the matching movie
            for (String[] row : allData) {
                if (row.length >= 10 && row[1].equalsIgnoreCase(movieName)) {
                    movieDetails.put("name", row[1]);
                    movieDetails.put("rating", row[6]); // Assuming column 6 has the rating
                    movieDetails.put("description", row[7]); // Assuming column 7 has the description
                    movieDetails.put("director", row[8]); // Assuming column 8 has the director
                    movieDetails.put("actors", row[10]); // Assuming column 10 has the actors
                    break; // Stop once we find the matching movie
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return movieDetails;
    }

    /**
     * Reads embeddings from a CSV file and stores them in a map.
     *
     * @param filePath The path to the embeddings CSV file.
     * @return A map where the key is the movie name, and the value is the corresponding embedding.
     */
    public static Map<String, String> readEmbeddings(String filePath) {
        Map<String, String> embeddingsMap = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;

            // Skip the header line
            reader.readLine();

            // Read each line of the embeddings file
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", -1); // Split movie name and embedding

                if (parts.length == 2) {
                    String movieName = parts[0].replace("\"", "").trim(); // Clean movie name
                    String embedding = parts[1].replace("\"", "").trim(); // Clean embedding

                    if (!movieName.isEmpty() && !embedding.isEmpty()) {
                        // Restore embedding format (replace | with comma)
                        String restoredEmbedding = embedding.replace("|", ",");
                        embeddingsMap.put(movieName, restoredEmbedding);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return embeddingsMap;
    }

    /**
     * Extracts details of a specific movie and generates an embedding via an external API.
     *
     * @param file       The path to the CSV file.
     * @param movieName  The name of the movie to fetch details for.
     * @return A string representing the generated embedding for the movie's director and actors.
     */
    public static String extractMovieDetails(String file, String movieName) {
        try {
            List<String[]> allData = readCsv(file);
            for (String[] row : allData) {
                if (row.length >= 10 && row[1].equalsIgnoreCase(movieName)) {
                    // Print movie details
                    System.out.println("Movie: " + row[1]);
                    System.out.println("Director: " + row[8]);
                    System.out.println("Actors: " + row[10]);

                    // Generate embedding by calling the API
                    return api.generateEmbedding(row[8], row[10]);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

class api {

    private static final String API_URL = "http://localhost:11434/api/embeddings"; // API URL for embedding generation

    /**
     * Generates an embedding for a movie based on its director and actors by calling an external API.
     *
     * @param director The director of the movie.
     * @param actors   The actors of the movie.
     * @return The generated embedding as a string.
     * @throws Exception If an error occurs during the HTTP request.
     */
    public static String generateEmbedding(String director, String actors) throws Exception {
        // Create JSON input string dynamically based on movie data
        String jsonInputString = createJsonPayload(director, actors);

        HttpClient client = HttpClient.newHttpClient();

        // Build HTTP POST request to the API
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonInputString))
                .build();

        // Send the request and get the response
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return response.body();
    }

    /**
     * Creates a JSON payload to send to the embedding generation API.
     *
     * @param director The director of the movie.
     * @param actors   The actors of the movie.
     * @return A JSON string representing the input for the API.
     */
    private static String createJsonPayload(String director, String actors) {
        // Construct a JSON object with model and prompt (director and actors)
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("model", "nomic-embed-text");
        jsonObject.put("prompt", director + " " + actors); // Combine director and actors for embedding

        return jsonObject.toString();
    }
}
