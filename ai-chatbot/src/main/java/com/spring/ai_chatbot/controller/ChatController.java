package com.spring.ai_chatbot.controller;

import com.spring.ai_chatbot.dto.request.ChatRequestDTO;
import com.spring.ai_chatbot.dto.response.*;
import com.spring.ai_chatbot.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * Standard Chat (General LLM question answering with live weather support)
     */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> chat(@Valid @RequestBody ChatRequestDTO request) {
        try {
            String answer = chatService.chat(request.message());
            return ResponseEntity.ok(new ChatResponseDTO(answer));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error processing chat request"));
        }
    }

    /**
     * RAG Chat (Question answering grounded in Pinecone Vector DB)
     */
    @PostMapping(path = "/rag", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> chatWithRag(@Valid @RequestBody ChatRequestDTO request) {
        try {
            RagResponseDTO response = chatService.chatWithRag(request.message());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error processing RAG query"));
        }
    }

    /**
     * Upload & Index PDF file into Pinecone Vector Store
     */
    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> uploadPdf(@RequestParam("file") MultipartFile file) {
        try {
            PdfIndexResultDTO result = chatService.uploadAndIndexPdf(file);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Failed to process PDF"));
        }
    }

    /**
     * Upload & Summarize PDF file
     */
    @PostMapping(path = "/summarize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> summarizePdf(@RequestParam("file") MultipartFile file) {
        try {
            PdfSummaryResultDTO result = chatService.summarizePdf(file);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Failed to summarize PDF"));
        }
    }

    /**
     * Health & Vector DB Connection Status
     */
    @GetMapping(path = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SystemStatusDTO> getStatus() {
        return ResponseEntity.ok(chatService.getStatus());
    }
}
