package com.spring.ai_chatbot.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ChatRequestDTO(
        @NotBlank(message = "Message cannot be empty")
        String message
) {}
