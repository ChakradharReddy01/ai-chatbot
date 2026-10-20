package com.spring.ai_chatbot.service;

import com.spring.ai_chatbot.config.VectorStoreConfig;
import com.spring.ai_chatbot.dto.response.*;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private static final Pattern WEATHER_PATTERN = Pattern.compile(
            "(?i)(?:weather|temperature|forecast|raining|climate)\\s+(?:in|for|at|of)\\s+([a-zA-Z\\s]{2,30})"
    );

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final VectorStoreConfig vectorStoreConfig;
    private final WeatherService weatherService;
    private final TokenTextSplitter tokenTextSplitter;

    @Value("${spring.ai.openai.chat.api-key:}")
    private String groqApiKey;

    public ChatService(ChatClient.Builder chatClientBuilder,
                       VectorStore vectorStore,
                       VectorStoreConfig vectorStoreConfig,
                       WeatherService weatherService) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
        this.vectorStoreConfig = vectorStoreConfig;
        this.weatherService = weatherService;
        this.tokenTextSplitter = TokenTextSplitter.builder().build();
    }

    @PostConstruct
    public void init() {
        if (StringUtils.hasText(groqApiKey)) {
            log.info("Groq API Key detected successfully! (starts with '{}...')",
                    groqApiKey.substring(0, Math.min(8, groqApiKey.length())));
        } else {
            log.warn("Groq API Key was NOT detected! Please verify GROQ_API_KEY in your IntelliJ Run Configuration.");
        }
    }

    /**
     * General chat conversation with live weather intelligence
     */
    public String chat(String message) {
        log.info("Processing general chat question: {}", message);
        checkGroqKeyConfigured();

        String weatherContext = tryExtractWeatherContext(message);

        var promptRequest = chatClient.prompt();
        if (weatherContext != null) {
            promptRequest = promptRequest.system(
                    "You are a helpful AI assistant. You have access to real-time live third-party weather data:\n" +
                    weatherContext + "\n" +
                    "Use this live information to answer the user's inquiry accurately, mentioning the live temperature, conditions, and source."
            );
        }

        return promptRequest
                .user(message)
                .call()
                .content();
    }

    /**
     * Upload a PDF file, extract text, split into token chunks, and index into Vector DB.
     */
    public PdfIndexResultDTO uploadAndIndexPdf(MultipartFile file) throws IOException {
        validatePdfFile(file);

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document.pdf";
        log.info("Processing PDF upload for indexing: {} ({} bytes)", originalFilename, file.getSize());

        byte[] bytes = file.getBytes();
        Resource resource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return originalFilename;
            }
        };

        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource, PdfDocumentReaderConfig.defaultConfig());
        List<Document> rawDocuments = pdfReader.get();
        int pageCount = rawDocuments.size();

        if (rawDocuments.isEmpty()) {
            throw new IllegalArgumentException("The uploaded PDF does not contain extractable text content.");
        }

        // Split into chunks suitable for embedding models
        List<Document> splitDocuments = tokenTextSplitter.apply(rawDocuments);

        // Enrich chunk metadata
        long timestamp = Instant.now().toEpochMilli();
        for (int i = 0; i < splitDocuments.size(); i++) {
            Document doc = splitDocuments.get(i);
            Map<String, Object> metadata = doc.getMetadata();
            metadata.put("source", originalFilename);
            metadata.put("chunk_index", i);
            metadata.put("total_chunks", splitDocuments.size());
            metadata.put("upload_time", timestamp);
        }

        // Add to Vector Store (Pinecone if configured, or offline in-memory store)
        vectorStore.add(splitDocuments);
        log.info("Successfully indexed {} chunks from '{}' ({} pages) using local offline embeddings",
                splitDocuments.size(), originalFilename, pageCount);

        String targetStore = vectorStoreConfig.isPineconeConfigured() ? "Pinecone Vector DB" : "In-Memory Vector Store";
        return new PdfIndexResultDTO(
                originalFilename,
                pageCount,
                splitDocuments.size(),
                "Successfully parsed " + pageCount + " pages and stored " + splitDocuments.size() + " vector chunks into " + targetStore + " using free offline embeddings."
        );
    }

    /**
     * Ask question with RAG context retrieved from Vector DB
     */
    public RagResponseDTO chatWithRag(String query) {
        log.info("Performing similarity search for query: {}", query);
        checkGroqKeyConfigured();

        List<Document> similarDocs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(4)
                        .similarityThreshold(0.5)
                        .build()
        );

        if (similarDocs == null || similarDocs.isEmpty()) {
            similarDocs = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(query)
                            .topK(4)
                            .build()
            );
        }

        if (similarDocs == null || similarDocs.isEmpty()) {
            String fallback = chatClient.prompt()
                    .system("You are a helpful AI assistant. The user's query did not match any indexed documents in the Vector database, so inform them gently and provide a helpful general answer.")
                    .user(query)
                    .call()
                    .content();
            return new RagResponseDTO(fallback, List.of(), false);
        }

        StringBuilder contextBuilder = new StringBuilder();
        List<SourceInfoDTO> sources = new ArrayList<>();

        for (int i = 0; i < similarDocs.size(); i++) {
            Document doc = similarDocs.get(i);
            String sourceName = String.valueOf(doc.getMetadata().getOrDefault("source", "Document " + (i + 1)));
            Object page = doc.getMetadata().getOrDefault("page_number", doc.getMetadata().getOrDefault("start_page_number", "N/A"));
            contextBuilder.append(String.format("--- [Source %d: %s | Page: %s] ---\n", (i + 1), sourceName, page));
            contextBuilder.append(doc.getText()).append("\n\n");

            sources.add(new SourceInfoDTO(sourceName, String.valueOf(page), doc.getText()));
        }

        String systemPrompt = """
                You are a knowledgeable and precise AI assistant answering questions based on the provided reference context extracted from uploaded documents.
                
                Instructions:
                1. Answer the user question accurately using the context provided below.
                2. If the context does not contain the answer, state honestly that the document does not mention it, and offer general knowledge if appropriate.
                3. Cite the source document or page where the information was found whenever possible.
                
                Reference Context:
                %s
                """.formatted(contextBuilder.toString());

        String answer = chatClient.prompt()
                .system(systemPrompt)
                .user(query)
                .call()
                .content();

        return new RagResponseDTO(answer, sources, true);
    }

    /**
     * Upload and summarize a PDF document directly
     */
    public PdfSummaryResultDTO summarizePdf(MultipartFile file) throws IOException {
        validatePdfFile(file);
        checkGroqKeyConfigured();

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document.pdf";
        log.info("Summarizing PDF: {}", originalFilename);

        byte[] bytes = file.getBytes();
        Resource resource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return originalFilename;
            }
        };

        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource, PdfDocumentReaderConfig.defaultConfig());
        List<Document> documents = pdfReader.get();

        if (documents.isEmpty()) {
            throw new IllegalArgumentException("The uploaded PDF does not contain extractable text content.");
        }

        StringBuilder fullText = new StringBuilder();
        for (Document doc : documents) {
            fullText.append(doc.getText()).append("\n");
            if (fullText.length() > 60000) {
                fullText.append("\n...[Document text truncated for summarization length]...\n");
                break;
            }
        }

        String prompt = """
                Provide an executive, structured summary of the following PDF document.
                Format your response in clean Markdown with the following sections:
                
                ## 📌 Executive Summary
                A concise 2-3 sentence overview of what the document is about.
                
                ## 🎯 Key Highlights & Major Themes
                Core themes, goals, or subjects discussed.
                
                ## 🔍 Detailed Insights
                Key points, data, or arguments presented in clear bullet points.
                
                ## 💡 Conclusions & Takeaways
                Final takeaways or conclusions from the document.
                
                ---
                Document Content:
                %s
                """.formatted(fullText.toString());

        String summary = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        return new PdfSummaryResultDTO(originalFilename, documents.size(), summary);
    }

    public SystemStatusDTO getStatus() {
        return new SystemStatusDTO(
                vectorStoreConfig.isPineconeConfigured() ? "Pinecone Vector DB" : "In-Memory SimpleVectorStore (Fallback)",
                vectorStoreConfig.isPineconeConfigured(),
                vectorStoreConfig.getIndexName(),
                "all-MiniLM-L6-v2 (Local ONNX, 384 dims, 100% Free/Offline)"
        );
    }

    private String tryExtractWeatherContext(String message) {
        if (!StringUtils.hasText(message)) return null;
        try {
            Matcher matcher = WEATHER_PATTERN.matcher(message);
            if (matcher.find()) {
                String rawCity = matcher.group(1).trim().replaceAll("[?.!,]+$", "");
                if (StringUtils.hasText(rawCity)) {
                    log.info("Detected weather inquiry for city '{}', fetching live 3rd-party weather data...", rawCity);
                    WeatherResponseDTO weather = weatherService.getCurrentWeather(rawCity);
                    return String.format(
                            "[Live Weather Data for %s, %s]: Temperature: %.1f°C (%.1f°F), Condition: %s, Humidity: %d%%, Wind: %.1f km/h, Feels Like: %.1f°C (Data Provider: %s)",
                            weather.city(), weather.country(),
                            weather.temperatureC() != null ? weather.temperatureC() : 0.0,
                            weather.temperatureF() != null ? weather.temperatureF() : 0.0,
                            weather.conditionText(),
                            weather.humidity() != null ? weather.humidity() : 0,
                            weather.windKph() != null ? weather.windKph() : 0.0,
                            weather.feelsLikeC() != null ? weather.feelsLikeC() : 0.0,
                            weather.provider()
                    );
                }
            }
        } catch (Exception e) {
            log.warn("Could not retrieve live weather context: {}", e.getMessage());
        }
        return null;
    }

    private void checkGroqKeyConfigured() {
        if (!StringUtils.hasText(groqApiKey)) {
            throw new IllegalStateException("Groq API Key is not set! Please check your IntelliJ Run Configuration Environment Variables (set GROQ_API_KEY).");
        }
    }

    private void validatePdfFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please provide a valid, non-empty PDF file.");
        }
        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();
        boolean isPdf = (contentType != null && contentType.equalsIgnoreCase("application/pdf"))
                || (filename != null && filename.toLowerCase().endsWith(".pdf"));
        if (!isPdf) {
            throw new IllegalArgumentException("Only PDF files (.pdf) are supported.");
        }
    }
}
