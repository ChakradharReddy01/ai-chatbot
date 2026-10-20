package com.spring.ai_chatbot.dto.request;

import jakarta.validation.constraints.NotBlank;

public record WeatherRequestDTO(
        @NotBlank(message = "City name cannot be empty")
        String city,
        String question
) {}
