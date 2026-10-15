package com.spring.ai_chatbot.controller;

import com.spring.ai_chatbot.service.ChatService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping(produces = "application/json;charset=UTF-8")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String answer = chatService.chat(request.message());
        return new ChatResponse(answer);
    }

    public record ChatRequest(String message) {}

    public record ChatResponse(String response) {}
}
