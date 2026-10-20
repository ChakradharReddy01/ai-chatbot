package com.spring.ai_chatbot.dto.response;

public record PdfIndexResultDTO(
        String fileName,
        int pageCount,
        int chunkCount,
        String message
) {}
