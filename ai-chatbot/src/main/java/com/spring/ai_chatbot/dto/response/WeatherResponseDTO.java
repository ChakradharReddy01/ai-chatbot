package com.spring.ai_chatbot.dto.response;

public record WeatherResponseDTO(
        String city,
        String region,
        String country,
        Double temperatureC,
        Double temperatureF,
        String conditionText,
        String conditionIcon,
        Integer humidity,
        Double windKph,
        Double feelsLikeC,
        Double uvIndex,
        String provider,
        String lastUpdated,
        String aiCommentary
) {}
