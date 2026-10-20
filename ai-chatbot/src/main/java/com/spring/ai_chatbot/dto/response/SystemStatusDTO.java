package com.spring.ai_chatbot.dto.response;

public record SystemStatusDTO(
        String vectorStoreType,
        boolean pineconeConnected,
        String indexName,
        String embeddingModel
) {}
