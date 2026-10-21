package com.spring.ai_chatbot.dto.response;

import java.util.List;

public record TravelPlanResponseDTO(
        String destination,
        Integer days,
        String budget,
        String travelStyle,
        WeatherResponseDTO currentDestinationWeather,
        String overview,
        List<DayPlanDTO> itinerary,
        List<String> topAttractions,
        List<String> foodRecommendations,
        List<String> packingTips,
        String source
) {
    public record DayPlanDTO(
            int dayNumber,
            String theme,
            String morning,
            String afternoon,
            String evening
    ) {}
}
