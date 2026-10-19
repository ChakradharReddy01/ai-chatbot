package com.spring.ai_chatbot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformers.TransformersEmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pinecone.PineconeVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

@Configuration
public class VectorStoreConfig {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreConfig.class);

    @Value("${spring.ai.vectorstore.pinecone.api-key:}")
    private String pineconeApiKey;

    @Value("${spring.ai.vectorstore.pinecone.index-name:}")
    private String pineconeIndexName;

    @Value("${spring.ai.vectorstore.pinecone.namespace:}")
    private String pineconeNamespace;

    /**
     * Free, 100% offline local embedding model (ONNX all-MiniLM-L6-v2, 384 dimensions).
     * Requires ZERO OpenAI API key, zero cost, completely local.
     */
    @Bean
    @Primary
    public EmbeddingModel localEmbeddingModel() {
        log.info("Initializing free local offline EmbeddingModel (ONNX all-MiniLM-L6-v2, 384 dimensions)...");
        TransformersEmbeddingModel model = new TransformersEmbeddingModel();
        try {
            model.afterPropertiesSet();
            log.info("Local offline EmbeddingModel successfully initialized.");
        } catch (Exception e) {
            log.error("Failed to initialize TransformersEmbeddingModel: {}", e.getMessage(), e);
            throw new IllegalStateException("Error initializing local embedding model", e);
        }
        return model;
    }

    @Bean
    @ConditionalOnMissingBean(VectorStore.class)
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        if (StringUtils.hasText(pineconeApiKey) && StringUtils.hasText(pineconeIndexName)) {
            log.info("Initializing Pinecone Vector Store (Index: '{}', Namespace: '{}')",
                    pineconeIndexName, pineconeNamespace);
            return PineconeVectorStore.builder(embeddingModel)
                    .apiKey(pineconeApiKey)
                    .indexName(pineconeIndexName)
                    .namespace(pineconeNamespace != null ? pineconeNamespace : "")
                    .build();
        } else {
            log.warn("Pinecone credentials not fully configured. Using in-memory SimpleVectorStore with local embeddings.");
            return SimpleVectorStore.builder(embeddingModel).build();
        }
    }

    public boolean isPineconeConfigured() {
        return StringUtils.hasText(pineconeApiKey) && StringUtils.hasText(pineconeIndexName);
    }

    public String getIndexName() {
        return pineconeIndexName;
    }
}
