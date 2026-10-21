package com.spring.ai_chatbot.dto.response;

import java.util.List;

public record ChatResponseDTO(
        String response,
        List<ToolExecutionDTO> toolCalls
) {
    public ChatResponseDTO(String response) {
        this(response, List.of());
    }

    public record ToolExecutionDTO(
            String toolName,
            String arguments,
            String resultSummary
    ) {}
}
