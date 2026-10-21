package com.spring.ai_chatbot.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TravelPlanRequestDTO(
        @NotBlank(message = "Destination cannot be empty")
        String destination,
        Integer days,
        String budget,
        String travelStyle,
        String interests
) {}
