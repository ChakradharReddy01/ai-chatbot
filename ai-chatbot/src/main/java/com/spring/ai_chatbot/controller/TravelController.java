package com.spring.ai_chatbot.controller;

import com.spring.ai_chatbot.dto.request.TravelPlanRequestDTO;
import com.spring.ai_chatbot.dto.response.TravelPlanResponseDTO;
import com.spring.ai_chatbot.entity.TravelPlanEntity;
import com.spring.ai_chatbot.service.TravelPlannerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/travel")
@CrossOrigin(origins = "*")
public class TravelController {

    private final TravelPlannerService travelPlannerService;

    public TravelController(TravelPlannerService travelPlannerService) {
        this.travelPlannerService = travelPlannerService;
    }

    /**
     * Generate an AI-powered, weather-aware travel itinerary
     */
    @PostMapping(path = "/plan", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> planTrip(@Valid @RequestBody TravelPlanRequestDTO request) {
        try {
            TravelPlanResponseDTO plan = travelPlannerService.planTripFromRequest(request);
            return ResponseEntity.ok(plan);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Failed to generate travel plan."));
        }
    }

    /**
     * Retrieve recent travel plans saved in H2 Database
     */
    @GetMapping(path = "/history", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TravelPlanEntity>> getHistory() {
        return ResponseEntity.ok(travelPlannerService.getRecentPlans());
    }
}
