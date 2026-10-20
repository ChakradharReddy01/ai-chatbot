package com.spring.ai_chatbot.dto.response;

import java.util.List;

public record RagResponseDTO(
        String answer,
        List<SourceInfoDTO> sources,
        boolean contextFound
) {}
