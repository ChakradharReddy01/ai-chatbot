package com.spring.ai_chatbot.controller;

import com.spring.ai_chatbot.dto.request.WeatherRequestDTO;
import com.spring.ai_chatbot.dto.response.WeatherResponseDTO;
import com.spring.ai_chatbot.entity.WeatherLogEntity;
import com.spring.ai_chatbot.service.WeatherService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/weather")
@CrossOrigin(origins = "*")
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    /**
     * Get real-time live weather for any city
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getWeather(@RequestParam("city") String city) {
        if (city == null || city.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "City query parameter is required."));
        }
        try {
            WeatherResponseDTO response = weatherService.getCurrentWeather(city);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Failed to fetch live weather data."));
        }
    }

    /**
     * Ask an AI question grounded in live weather data
     */
    @PostMapping(path = "/ask", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> askAboutWeather(@Valid @RequestBody WeatherRequestDTO request) {
        try {
            WeatherResponseDTO response = weatherService.askAboutWeather(request.city(), request.question());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Failed to process weather query."));
        }
    }

    /**
     * Get recent weather query history logged into H2 Database
     */
    @GetMapping(path = "/history", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<WeatherLogEntity>> getHistory() {
        return ResponseEntity.ok(weatherService.getRecentQueries());
    }
}
