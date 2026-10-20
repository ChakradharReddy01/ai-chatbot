package com.spring.ai_chatbot.dto.response;

public record PdfSummaryResultDTO(
        String fileName,
        int pageCount,
        String summary
) {}
