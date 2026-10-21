package com.spring.ai_chatbot.service;

import com.spring.ai_chatbot.dto.request.TravelPlanRequestDTO;
import com.spring.ai_chatbot.dto.response.TravelPlanResponseDTO;
import com.spring.ai_chatbot.dto.response.WeatherResponseDTO;
import com.spring.ai_chatbot.entity.TravelPlanEntity;
import com.spring.ai_chatbot.repository.TravelPlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class TravelPlannerService {

    private static final Logger log = LoggerFactory.getLogger(TravelPlannerService.class);

    private final WeatherService weatherService;
    private final TravelPlanRepository travelPlanRepository;
    private final ChatClient chatClient;

    public TravelPlannerService(WeatherService weatherService,
                                TravelPlanRepository travelPlanRepository,
                                ChatClient.Builder chatClientBuilder) {
        this.weatherService = weatherService;
        this.travelPlanRepository = travelPlanRepository;
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * Plan a comprehensive travel itinerary grounded in real-time destination weather.
     * Annotated with @Tool so the LLM can invoke it automatically!
     */
    @Tool(name = "planTrip", description = "Plan a detailed, weather-aware travel itinerary for a destination including daily activities, attractions, and food recommendations.")
    public TravelPlanResponseDTO planTrip(
            @ToolParam(description = "Destination city or region, e.g. Kyoto, Rome, Bali, New York") String destination,
            @ToolParam(description = "Trip duration in days (default 3)") Integer days,
            @ToolParam(description = "Budget level: Budget, Moderate, or Luxury") String budget,
            @ToolParam(description = "Travel style: Adventure, Relaxation, Culture, Foodie, Family") String travelStyle,
            @ToolParam(description = "Special interests or user preferences") String interests) {

        int validDays = (days == null || days <= 0) ? 3 : Math.min(days, 14);
        String validBudget = StringUtils.hasText(budget) ? budget : "Moderate";
        String validStyle = StringUtils.hasText(travelStyle) ? travelStyle : "Culture & Sightseeing";

        log.info("✈️ [TRAVEL] Planning {}-day trip to '{}' (Budget: {}, Style: {})", validDays, destination, validBudget, validStyle);

        // Fetch live weather for destination to ground the itinerary
        WeatherResponseDTO weather = null;
        try {
            weather = weatherService.getCurrentWeather(destination);
            log.info("🌦️ [TRAVEL] Integrated live weather for {}: {}°C, {}",
                    destination, weather.temperatureC(), weather.conditionText());
        } catch (Exception e) {
            log.warn("Could not retrieve live weather for destination '{}': {}", destination, e.getMessage());
        }

        String weatherSummary = (weather != null)
                ? String.format("%s, %s: %.1f°C (%.1f°F), %s, Humidity: %d%%, Wind: %.1f km/h",
                    weather.city(), weather.country(), weather.temperatureC(), weather.temperatureF(),
                    weather.conditionText(), weather.humidity(), weather.windKph())
                : "Pleasant seasonal conditions";

        String prompt = """
                You are an expert travel concierge and trip planner.
                Create an inspiring, weather-tailored %d-day itinerary for "%s".

                Context & Preferences:
                - Current Live Destination Weather: %s
                - Trip Duration: %d days
                - Budget Level: %s
                - Travel Style: %s
                - Special Interests: %s

                Format your response in Markdown with these EXACT sections:
                ## 🗺️ Trip Overview
                A 2-3 sentence teaser and weather-aware travel advisory.

                ## 📅 Day-by-Day Itinerary
                ### Day 1: [Theme]
                - **Morning:** [Specific activity]
                - **Afternoon:** [Specific activity]
                - **Evening:** [Dining / sunset experience]

                (Repeat for each day up to Day %d)

                ## 🏛️ Top Attractions
                - Bullet list of 4-5 must-see landmarks.

                ## 🍜 Food & Dining
                - 3-4 iconic local dishes or recommended dining districts.

                ## 🎒 Weather & Packing Tips
                - Specific gear, clothing, and footwear recommendations matched to the current weather (%s).
                """.formatted(
                validDays, destination, weatherSummary, validDays, validBudget, validStyle,
                StringUtils.hasText(interests) ? interests : "Must-see highlights",
                validDays, weatherSummary
        );

        String fullMarkdown = chatClient.prompt().user(prompt).call().content();

        // Extract structured sections
        String overview = extractSection(fullMarkdown, "## 🗺️ Trip Overview", "## 📅");
        List<TravelPlanResponseDTO.DayPlanDTO> itinerary = parseDays(fullMarkdown, validDays);
        List<String> attractions = parseBulletList(extractSection(fullMarkdown, "## 🏛️ Top Attractions", "## 🍜"));
        List<String> food = parseBulletList(extractSection(fullMarkdown, "## 🍜 Food & Dining", "## 🎒"));
        List<String> packing = parseBulletList(extractSection(fullMarkdown, "## 🎒 Weather & Packing Tips", "---"));

        TravelPlanResponseDTO responseDTO = new TravelPlanResponseDTO(
                destination,
                validDays,
                validBudget,
                validStyle,
                weather,
                overview != null ? overview.trim() : "Custom " + validDays + "-day trip to " + destination,
                itinerary,
                attractions,
                food,
                packing,
                weather != null ? weather.provider() : "AI Travel Engine"
        );

        // Persist to H2 Database
        try {
            TravelPlanEntity entity = new TravelPlanEntity(
                    destination,
                    validDays,
                    validBudget,
                    validStyle,
                    weather != null ? weather.temperatureC() : null,
                    weather != null ? weather.conditionText() : null,
                    overview,
                    fullMarkdown,
                    LocalDateTime.now()
            );
            travelPlanRepository.save(entity);
            log.info("💾 [DATABASE] Saved travel plan for '{}' to H2 table 'travel_plans'", destination);
        } catch (Exception e) {
            log.error("Failed to save travel plan to database: {}", e.getMessage());
        }

        return responseDTO;
    }

    public TravelPlanResponseDTO planTripFromRequest(TravelPlanRequestDTO request) {
        return planTrip(
                request.destination(),
                request.days(),
                request.budget(),
                request.travelStyle(),
                request.interests()
        );
    }

    public List<TravelPlanEntity> getRecentPlans() {
        return travelPlanRepository.findTop10ByOrderByCreatedTimestampDesc();
    }

    private String extractSection(String text, String startHeader, String endHeader) {
        if (text == null) return "";
        int start = text.indexOf(startHeader);
        if (start == -1) return "";
        start += startHeader.length();
        int end = (endHeader != null && text.indexOf(endHeader, start) != -1)
                ? text.indexOf(endHeader, start)
                : text.length();
        return text.substring(start, end).trim();
    }

    private List<TravelPlanResponseDTO.DayPlanDTO> parseDays(String text, int maxDays) {
        List<TravelPlanResponseDTO.DayPlanDTO> list = new ArrayList<>();
        if (text == null) return list;

        for (int i = 1; i <= maxDays; i++) {
            String dayHeader = "### Day " + i;
            String nextHeader = (i < maxDays) ? "### Day " + (i + 1) : "## ";
            String dayBlock = extractSection(text, dayHeader, nextHeader);
            if (!StringUtils.hasText(dayBlock)) continue;

            String theme = "";
            int colonIdx = dayBlock.indexOf(":");
            int lineEnd = dayBlock.indexOf("\n");
            if (colonIdx != -1 && colonIdx < lineEnd) {
                theme = dayBlock.substring(colonIdx + 1, lineEnd).trim();
            }

            String morning = extractBullet(dayBlock, "Morning:");
            String afternoon = extractBullet(dayBlock, "Afternoon:");
            String evening = extractBullet(dayBlock, "Evening:");

            list.add(new TravelPlanResponseDTO.DayPlanDTO(
                    i,
                    theme.isEmpty() ? "Explore & Discover" : theme,
                    morning.isEmpty() ? "Morning city sightseeing and breakfast" : morning,
                    afternoon.isEmpty() ? "Afternoon cultural landmark exploration" : afternoon,
                    evening.isEmpty() ? "Evening dinner and relaxation" : evening
            ));
        }
        return list;
    }

    private String extractBullet(String block, String label) {
        int idx = block.indexOf(label);
        if (idx == -1) return "";
        int start = idx + label.length();
        int end = block.indexOf("\n", start);
        if (end == -1) end = block.length();
        return block.substring(start, end).trim().replaceAll("^[*\\s-]+", "");
    }

    private List<String> parseBulletList(String block) {
        List<String> items = new ArrayList<>();
        if (block == null) return items;
        for (String line : block.split("\n")) {
            String clean = line.trim().replaceAll("^[*\\s-]+", "");
            if (StringUtils.hasText(clean) && !clean.startsWith("#")) {
                items.add(clean);
            }
        }
        return items;
    }
}
