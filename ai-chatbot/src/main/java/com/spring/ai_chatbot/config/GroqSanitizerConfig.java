package com.spring.ai_chatbot.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.openai.http.okhttp.OpenAiHttpClientBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/*
 * Custom OkHttp client customizer for Groq API integration.
 * Groq's Chat Completions endpoint strictly validates request payloads and rejects
* incoming assistant messages that include 'reasoning_content' or 'reasoning'
* (which Spring AI serializes during multi-turn tool calling conversations).
 * This interceptor sanitizes the JSON body before sending it to api.groq.com.
*/
@Configuration
public class GroqSanitizerConfig {

    private static final Logger log = LoggerFactory.getLogger(GroqSanitizerConfig.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Bean
    public OpenAiHttpClientBuilderCustomizer groqOpenAiHttpClientCustomizer() {
        return builder -> {
            log.info("Registering Groq request sanitizer interceptor to strip unsupported fields");
            builder.interceptor(new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Request request = chain.request();
                    RequestBody body = request.body();

                    if (body != null && request.url().toString().contains("groq.com")) {
                        try {
                            Buffer buffer = new Buffer();
                            body.writeTo(buffer);
                            String rawJson = buffer.readUtf8();

                            if (rawJson.contains("reasoning_content") || rawJson.contains("\"reasoning\":")) {
                                JsonNode root = objectMapper.readTree(rawJson);
                                boolean modified = false;

                                if (root.has("messages") && root.get("messages").isArray()) {
                                    ArrayNode messages = (ArrayNode) root.get("messages");
                                    for (JsonNode message : messages) {
                                        if (message.isObject()) {
                                            ObjectNode msgObj = (ObjectNode) message;
                                           if (msgObj.has("reasoning_content")) {
                                                msgObj.remove("reasoning_content");
                                                modified = true;
                                            }
                                            if (msgObj.has("reasoning")) {
                                                msgObj.remove("reasoning");
                                                modified = true;
                                            }
                                        }
                                    }
                                }

                                if (modified) {
                                    log.debug("Sanitized Groq payload by removing unsupported 'reasoning_content'");
                                    byte[] modifiedBytes = objectMapper.writeValueAsBytes(root);
                                    MediaType mediaType = body.contentType();
                                    RequestBody newBody = RequestBody.create(modifiedBytes, mediaType);
                                    Request newRequest = request.newBuilder()
                                           .method(request.method(), newBody)
                                           .build();
                                    return chain.proceed(newRequest);
                                }
                            }
                        } catch (Exception e) {
                            log.warn("Failed to sanitize Groq request, continuing: {}", e.getMessage());
                        }
                    }

                    return chain.proceed(request);
                }
            });
        };
    }
}
