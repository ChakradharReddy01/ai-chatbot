package com.spring.ai_chatbot.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "travel_plans")
public class TravelPlanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String destination;

    private Integer days;

    private String budget;

    private String travelStyle;

    private Double destinationTemperatureC;

    private String destinationWeatherCondition;

    @Column(length = 4000)
    private String overview;

    @Column(length = 8000)
    private String itineraryMarkdown;

    @Column(nullable = false)
    private LocalDateTime createdTimestamp;

    public TravelPlanEntity() {
    }

    public TravelPlanEntity(String destination, Integer days, String budget, String travelStyle,
                            Double destinationTemperatureC, String destinationWeatherCondition,
                            String overview, String itineraryMarkdown, LocalDateTime createdTimestamp) {
        this.destination = destination;
        this.days = days;
        this.budget = budget;
        this.travelStyle = travelStyle;
        this.destinationTemperatureC = destinationTemperatureC;
        this.destinationWeatherCondition = destinationWeatherCondition;
        this.overview = overview;
        this.itineraryMarkdown = itineraryMarkdown;
        this.createdTimestamp = createdTimestamp;
    }

    public Long getId() {
        return id;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public Integer getDays() {
        return days;
    }

    public void setDays(Integer days) {
        this.days = days;
    }

    public String getBudget() {
        return budget;
    }

    public void setBudget(String budget) {
        this.budget = budget;
    }

    public String getTravelStyle() {
        return travelStyle;
    }

    public void setTravelStyle(String travelStyle) {
        this.travelStyle = travelStyle;
    }

    public Double getDestinationTemperatureC() {
        return destinationTemperatureC;
    }

    public void setDestinationTemperatureC(Double destinationTemperatureC) {
        this.destinationTemperatureC = destinationTemperatureC;
    }

    public String getDestinationWeatherCondition() {
        return destinationWeatherCondition;
    }

    public void setDestinationWeatherCondition(String destinationWeatherCondition) {
        this.destinationWeatherCondition = destinationWeatherCondition;
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

    public String getItineraryMarkdown() {
        return itineraryMarkdown;
    }

    public void setItineraryMarkdown(String itineraryMarkdown) {
        this.itineraryMarkdown = itineraryMarkdown;
    }

    public LocalDateTime getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(LocalDateTime createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }
}
