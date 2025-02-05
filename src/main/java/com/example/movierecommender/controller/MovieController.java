package com.example.movierecommender.controller;

import com.example.movierecommender.service.MovieService;  // Importing MovieService to interact with movie recommendation logic
import org.springframework.web.bind.annotation.*;  // Importing annotations for the Spring Web framework
import org.springframework.http.ResponseEntity;  // Importing ResponseEntity for returning HTTP responses
import java.util.*;  // Importing utility classes for working with lists and maps

// Enable Cross-Origin Resource Sharing (CORS) from any origin, allowing frontend to make requests
@CrossOrigin(origins = "*")
@RestController  // Marks this class as a Spring controller that handles HTTP requests and returns responses
@RequestMapping("/api")  // All endpoints in this controller will start with "/api"
public class MovieController {

    private final MovieService movieService;  // Declaring MovieService as a private member variable

    // Constructor-based dependency injection for the MovieService
    public MovieController(MovieService movieService) {
        this.movieService = movieService;  // Initializing movieService with the provided MovieService instance
    }

    // HTTP GET endpoint to fetch movie recommendations
    @GetMapping("/recommend")
    public ResponseEntity<?> getRecommendations(
            @RequestParam(required = false) String name,  // 'name' parameter is optional in the request
            @RequestParam(required = false) String genre,  // 'genre' parameter is optional in the request
            @RequestParam(required = false, defaultValue = "5") int count) {  // 'count' parameter with a default value of 5

        // Validating if the 'name' or 'genre' are missing or empty
        if (name == null || genre == null || name.trim().isEmpty() || genre.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Missing required parameters: 'name' and 'genre'");  // Returning bad request if parameters are missing
        }

        // Logging the received parameters for debugging purposes
        System.out.println("Received request: name=" + name + ", genre=" + genre + ", count=" + count);

        // Calling the movieService to get the recommended movies based on the provided 'name', 'genre', and 'count'
        List<Map<String, String>> recommendations = movieService.getRecommendedMovies(name, genre, count);

        // Checking if there are no recommendations found and returning a 404 response if true
        if (recommendations.isEmpty()) {
            return ResponseEntity.status(404).body("No recommendations found.");
        }

        // Returning the recommendations in the response with an HTTP 200 status code
        return ResponseEntity.ok(recommendations);
    }
}

