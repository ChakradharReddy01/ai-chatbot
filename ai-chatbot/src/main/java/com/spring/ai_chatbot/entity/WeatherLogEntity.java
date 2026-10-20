package com.spring.ai_chatbot.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "weather_logs")
public class WeatherLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String city;

    private String country;

    private Double temperatureC;

    private Double temperatureF;

    private String conditionText;

    private String conditionIcon;

    private Integer humidity;

    private Double windKph;

    private String provider;

    @Column(nullable = false)
    private LocalDateTime queryTimestamp;

    private String userQuestion;

    @Column(length = 4000)
    private String aiResponse;

    public WeatherLogEntity() {
    }

    public WeatherLogEntity(String city, String country, Double temperatureC, Double temperatureF,
                            String conditionText, String conditionIcon, Integer humidity,
                            Double windKph, String provider, LocalDateTime queryTimestamp,
                            String userQuestion, String aiResponse) {
        this.city = city;
        this.country = country;
        this.temperatureC = temperatureC;
        this.temperatureF = temperatureF;
        this.conditionText = conditionText;
        this.conditionIcon = conditionIcon;
        this.humidity = humidity;
        this.windKph = windKph;
        this.provider = provider;
        this.queryTimestamp = queryTimestamp;
        this.userQuestion = userQuestion;
        this.aiResponse = aiResponse;
    }

    public Long getId() {
        return id;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Double getTemperatureC() {
        return temperatureC;
    }

    public void setTemperatureC(Double temperatureC) {
        this.temperatureC = temperatureC;
    }

    public Double getTemperatureF() {
        return temperatureF;
    }

    public void setTemperatureF(Double temperatureF) {
        this.temperatureF = temperatureF;
    }

    public String getConditionText() {
        return conditionText;
    }

    public void setConditionText(String conditionText) {
        this.conditionText = conditionText;
    }

    public String getConditionIcon() {
        return conditionIcon;
    }

    public void setConditionIcon(String conditionIcon) {
        this.conditionIcon = conditionIcon;
    }

    public Integer getHumidity() {
        return humidity;
    }

    public void setHumidity(Integer humidity) {
        this.humidity = humidity;
    }

    public Double getWindKph() {
        return windKph;
    }

    public void setWindKph(Double windKph) {
        this.windKph = windKph;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public LocalDateTime getQueryTimestamp() {
        return queryTimestamp;
    }

    public void setQueryTimestamp(LocalDateTime queryTimestamp) {
        this.queryTimestamp = queryTimestamp;
    }

    public String getUserQuestion() {
        return userQuestion;
    }

    public void setUserQuestion(String userQuestion) {
        this.userQuestion = userQuestion;
    }

    public String getAiResponse() {
        return aiResponse;
    }

    public void setAiResponse(String aiResponse) {
        this.aiResponse = aiResponse;
    }
}
