package com.spring.ai_chatbot.dto.response;

public record SourceInfoDTO(
        String document,
        String page,
        String snippet
) {}
