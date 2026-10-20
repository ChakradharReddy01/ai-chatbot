package com.spring.ai_chatbot.service;

import com.spring.ai_chatbot.dto.response.WeatherResponseDTO;
import com.spring.ai_chatbot.entity.WeatherLogEntity;
import com.spring.ai_chatbot.repository.WeatherLogRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class WeatherService {

    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);

    private final WeatherLogRepository weatherLogRepository;
    private final ChatClient chatClient;
    private final RestClient restClient;

    @Value("${spring.weather.api-key:}")
    private String weatherApiKey;

    @Value("${spring.weather.api-url:https://api.weatherapi.com/v1}")
    private String weatherApiUrl;

    public WeatherService(WeatherLogRepository weatherLogRepository,
                          ChatClient.Builder chatClientBuilder) {
        this.weatherLogRepository = weatherLogRepository;
        this.chatClient = chatClientBuilder.build();
        this.restClient = RestClient.builder().build();
    }

    @PostConstruct
    public void init() {
        if (StringUtils.hasText(weatherApiKey)) {
            log.info("WeatherAPI.com Key configured successfully! (starts with '{}...')",
                    weatherApiKey.substring(0, Math.min(6, weatherApiKey.length())));
        } else {
            log.info("WeatherAPI.com Key is not set in WEATHER_API_KEY. Defaulting to free live Open-Meteo weather provider.");
        }
    }

    /**
     * Get live weather for a city with persistence in H2
     */
    public WeatherResponseDTO getCurrentWeather(String city) {
        return fetchWeather(city, null);
    }

    /**
     * Ask question grounded in live weather data
     */
    public WeatherResponseDTO askAboutWeather(String city, String userQuestion) {
        return fetchWeather(city, userQuestion);
    }

    /**
     * Get recent queries from H2 database
     */
    public List<WeatherLogEntity> getRecentQueries() {
        return weatherLogRepository.findTop15ByOrderByQueryTimestampDesc();
    }

    private WeatherResponseDTO fetchWeather(String city, String userQuestion) {
        if (!StringUtils.hasText(city)) {
            throw new IllegalArgumentException("City name cannot be blank.");
        }
        String cleanCity = city.trim();
        log.info("Fetching live weather for city: '{}'", cleanCity);

        WeatherResponseDTO weatherDto;
        if (StringUtils.hasText(weatherApiKey)) {
            try {
                weatherDto = fetchFromWeatherApi(cleanCity);
            } catch (Exception e) {
                log.warn("WeatherAPI.com request failed for '{}' ({}), falling back to Open-Meteo: {}",
                        cleanCity, e.getMessage(), e.toString());
                weatherDto = fetchFromOpenMeteo(cleanCity);
            }
        } else {
            weatherDto = fetchFromOpenMeteo(cleanCity);
        }

        // If user question provided, generate AI insights
        String aiAdvice = null;
        if (StringUtils.hasText(userQuestion)) {
            aiAdvice = generateWeatherAdvice(weatherDto, userQuestion);
            weatherDto = new WeatherResponseDTO(
                    weatherDto.city(),
                    weatherDto.region(),
                    weatherDto.country(),
                    weatherDto.temperatureC(),
                    weatherDto.temperatureF(),
                    weatherDto.conditionText(),
                    weatherDto.conditionIcon(),
                    weatherDto.humidity(),
                    weatherDto.windKph(),
                    weatherDto.feelsLikeC(),
                    weatherDto.uvIndex(),
                    weatherDto.provider(),
                    weatherDto.lastUpdated(),
                    aiAdvice
            );
        }

        // Save to H2 Database
        try {
            WeatherLogEntity entity = new WeatherLogEntity(
                    weatherDto.city(),
                    weatherDto.country(),
                    weatherDto.temperatureC(),
                    weatherDto.temperatureF(),
                    weatherDto.conditionText(),
                    weatherDto.conditionIcon(),
                    weatherDto.humidity(),
                    weatherDto.windKph(),
                    weatherDto.provider(),
                    LocalDateTime.now(),
                    userQuestion,
                    aiAdvice
            );
            weatherLogRepository.save(entity);
            log.info("Saved weather query for '{}' to H2 database", weatherDto.city());
        } catch (Exception e) {
            log.error("Failed to save weather log to database: {}", e.getMessage());
        }

        return weatherDto;
    }

    @SuppressWarnings("unchecked")
    private WeatherResponseDTO fetchFromWeatherApi(String city) {
        String url = String.format("%s/current.json?key=%s&q=%s&aqi=no",
                weatherApiUrl, weatherApiKey.trim(), city);

        Map<String, Object> root = restClient.get()
                .uri(url)
                .retrieve()
                .body(Map.class);

        if (root == null || !root.containsKey("current")) {
            throw new IllegalStateException("Invalid response received from WeatherAPI.com");
        }

        Map<String, Object> location = (Map<String, Object>) root.get("location");
        Map<String, Object> current = (Map<String, Object>) root.get("current");
        Map<String, Object> condition = (Map<String, Object>) current.get("condition");

        String cityName = location != null ? String.valueOf(location.get("name")) : city;
        String region = location != null ? String.valueOf(location.get("region")) : "";
        String country = location != null ? String.valueOf(location.get("country")) : "";

        Double tempC = toDouble(current.get("temp_c"));
        Double tempF = toDouble(current.get("temp_f"));
        String conditionText = condition != null ? String.valueOf(condition.get("text")) : "Clear";
        String conditionIcon = condition != null ? String.valueOf(condition.get("icon")) : "";
        if (conditionIcon != null && conditionIcon.startsWith("//")) {
            conditionIcon = "https:" + conditionIcon;
        }

        Integer humidity = toInteger(current.get("humidity"));
        Double windKph = toDouble(current.get("wind_kph"));
        Double feelsLikeC = toDouble(current.get("feelslike_c"));
        Double uv = toDouble(current.get("uv"));
        String lastUpdated = current.get("last_updated") != null ? String.valueOf(current.get("last_updated")) : LocalDateTime.now().toString();

        return new WeatherResponseDTO(
                cityName,
                region,
                country,
                tempC,
                tempF,
                conditionText,
                conditionIcon,
                humidity,
                windKph,
                feelsLikeC,
                uv,
                "WeatherAPI.com",
                lastUpdated,
                null
        );
    }

    @SuppressWarnings("unchecked")
    private WeatherResponseDTO fetchFromOpenMeteo(String city) {
        // Step 1: Geocoding
        String geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name=" + city + "&count=1&language=en&format=json";
        Map<String, Object> geoResponse = restClient.get()
                .uri(geoUrl)
                .retrieve()
                .body(Map.class);

        if (geoResponse == null || !geoResponse.containsKey("results")) {
            throw new IllegalArgumentException("City '" + city + "' not found.");
        }

        List<Map<String, Object>> results = (List<Map<String, Object>>) geoResponse.get("results");
        if (results == null || results.isEmpty()) {
            throw new IllegalArgumentException("City '" + city + "' not found.");
        }

        Map<String, Object> first = results.get(0);
        String name = String.valueOf(first.get("name"));
        String country = first.get("country") != null ? String.valueOf(first.get("country")) : "";
        String region = first.get("admin1") != null ? String.valueOf(first.get("admin1")) : "";
        Double latitude = toDouble(first.get("latitude"));
        Double longitude = toDouble(first.get("longitude"));

        // Step 2: Forecast
        String forecastUrl = String.format(
                "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m",
                latitude, longitude);

        Map<String, Object> forecastResponse = restClient.get()
                .uri(forecastUrl)
                .retrieve()
                .body(Map.class);

        if (forecastResponse == null || !forecastResponse.containsKey("current")) {
            throw new IllegalStateException("Failed to retrieve current weather from Open-Meteo.");
        }

        Map<String, Object> current = (Map<String, Object>) forecastResponse.get("current");
        Double tempC = toDouble(current.get("temperature_2m"));
        Double tempF = tempC != null ? Math.round((tempC * 9.0 / 5.0 + 32.0) * 10.0) / 10.0 : null;
        Double feelsLikeC = toDouble(current.get("apparent_temperature"));
        Integer humidity = toInteger(current.get("relative_humidity_2m"));
        Double windKph = toDouble(current.get("wind_speed_10m"));
        Integer weatherCode = toInteger(current.get("weather_code"));

        String conditionText = mapWmoWeatherCode(weatherCode);
        String conditionIcon = getConditionIcon(weatherCode);

        String lastUpdated = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        return new WeatherResponseDTO(
                name,
                region,
                country,
                tempC,
                tempF,
                conditionText,
                conditionIcon,
                humidity,
                windKph,
                feelsLikeC,
                null,
                "Open-Meteo (Live API)",
                lastUpdated,
                null
        );
    }

    private String generateWeatherAdvice(WeatherResponseDTO weather, String question) {
        String prompt = """
                You are a smart weather assistant. Given the following live weather conditions:
                - Location: %s, %s
                - Temperature: %.1f°C (%.1f°F)
                - Condition: %s
                - Humidity: %d%%
                - Wind Speed: %.1f km/h
                - Feels Like: %.1f°C
                
                User Question: "%s"
                
                Provide an insightful, helpful, and concise answer directly addressing their question based on these exact weather conditions. Keep it within 3-4 sentences.
                """.formatted(
                weather.city(), weather.country(),
                weather.temperatureC() != null ? weather.temperatureC() : 0.0,
                weather.temperatureF() != null ? weather.temperatureF() : 0.0,
                weather.conditionText(),
                weather.humidity() != null ? weather.humidity() : 0,
                weather.windKph() != null ? weather.windKph() : 0.0,
                weather.feelsLikeC() != null ? weather.feelsLikeC() : 0.0,
                question
        );

        try {
            return chatClient.prompt().user(prompt).call().content();
        } catch (Exception e) {
            log.warn("AI advice generation failed: {}", e.getMessage());
            return "Current weather in " + weather.city() + " is " + weather.temperatureC() + "°C and " + weather.conditionText() + ".";
        }
    }

    private String mapWmoWeatherCode(Integer code) {
        if (code == null) return "Clear sky";
        return switch (code) {
            case 0 -> "Clear sky";
            case 1, 2, 3 -> "Partly cloudy";
            case 45, 48 -> "Foggy";
            case 51, 53, 55 -> "Drizzle";
            case 61, 63, 65 -> "Rain";
            case 71, 73, 75, 77 -> "Snow fall";
            case 80, 81, 82 -> "Rain showers";
            case 85, 86 -> "Snow showers";
            case 95 -> "Thunderstorm";
            case 96, 99 -> "Thunderstorm with hail";
            default -> "Overcast";
        };
    }

    private String getConditionIcon(Integer code) {
        if (code == null) return "https://cdn.weatherapi.com/weather/64x64/day/113.png";
        return switch (code) {
            case 0 -> "https://cdn.weatherapi.com/weather/64x64/day/113.png";
            case 1, 2, 3 -> "https://cdn.weatherapi.com/weather/64x64/day/116.png";
            case 45, 48 -> "https://cdn.weatherapi.com/weather/64x64/day/143.png";
            case 51, 53, 55, 61, 63, 65, 80, 81, 82 -> "https://cdn.weatherapi.com/weather/64x64/day/296.png";
            case 71, 73, 75, 77, 85, 86 -> "https://cdn.weatherapi.com/weather/64x64/day/338.png";
            case 95, 96, 99 -> "https://cdn.weatherapi.com/weather/64x64/day/389.png";
            default -> "https://cdn.weatherapi.com/weather/64x64/day/122.png";
        };
    }

    private Double toDouble(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number num) return num.doubleValue();
        try {
            return Double.parseDouble(String.valueOf(obj));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer toInteger(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number num) return num.intValue();
        try {
            return Integer.parseInt(String.valueOf(obj));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
