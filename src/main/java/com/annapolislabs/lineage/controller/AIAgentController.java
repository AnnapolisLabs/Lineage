package com.annapolislabs.lineage.controller;

import com.annapolislabs.lineage.dto.request.AIMessageRequest;
import com.annapolislabs.lineage.dto.response.AIChatListResponse;
import com.annapolislabs.lineage.dto.response.AIMessageResponse;
import com.annapolislabs.lineage.entity.AIConversation;
import com.annapolislabs.lineage.entity.User;
import com.annapolislabs.lineage.service.AIAgentService;
import com.annapolislabs.lineage.service.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for AI Agent operations
 */
@RestController
@RequestMapping("/api/ai")
public class AIAgentController {

    private static final Logger logger = LoggerFactory.getLogger(AIAgentController.class);

    private final AIAgentService aiAgentService;
    private final AuthService authService;
    
    public AIAgentController(AIAgentService aiAgentService, AuthService authService) {
        this.aiAgentService = aiAgentService;
        this.authService = authService;
    }
    
    /**
     * Send a message to the AI agent
     */
    @PostMapping("/chat")
    public ResponseEntity<AIMessageResponse> sendMessage(@Valid @RequestBody AIMessageRequest request) {
        try {
            User currentUser = authService.getCurrentUser();

            // Use provided chatId or create new one
            String chatId = request.getChatId();
            if (chatId == null || chatId.isBlank()) {
                chatId = aiAgentService.createNewConversation(currentUser);
            }

            logger.info("Processing AI message for user={}, chatId={}, projectId={}",
                    currentUser.getEmail(), chatId, request.getProjectId());

            // Process message
            String response = aiAgentService.processMessage(
                    chatId,
                    request.getMessage(),
                    request.getProjectId(),
                    currentUser
            );

            logger.info("AI response generated successfully");
            return ResponseEntity.ok(new AIMessageResponse(response, chatId, false, null));

        } catch (Exception e) {
            logger.error("Error processing AI message: {}", e.getMessage(), e);
            return ResponseEntity.ok(new AIMessageResponse(
                    null,
                    request.getChatId(),
                    false,
                    "Error: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Create a new conversation
     */
    @PostMapping("/chat/new")
    public ResponseEntity<String> createNewChat() {
        User currentUser = authService.getCurrentUser();
        String chatId = aiAgentService.createNewConversation(currentUser);
        return ResponseEntity.ok(chatId);
    }
    
    /**
     * Get conversation history for current user
     */
    @GetMapping("/chat/history")
    public ResponseEntity<List<AIChatListResponse>> getChatHistory() {
        User currentUser = authService.getCurrentUser();
        List<AIConversation> conversations = aiAgentService.getConversationHistory(currentUser);
        
        List<AIChatListResponse> response = conversations.stream()
                .map(conv -> new AIChatListResponse(
                        conv.getChatId(),
                        conv.getTitle(),
                        conv.getUpdatedAt(),
                        (int) conv.getMessages().stream()
                                .filter(m -> !"system".equals(m.getRole()))
                                .count()
                ))
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Delete a conversation
     */
    @DeleteMapping("/chat/{chatId}")
    public ResponseEntity<Void> deleteChat(@PathVariable String chatId) {
        User currentUser = authService.getCurrentUser();
        aiAgentService.deleteConversation(chatId, currentUser);
        return ResponseEntity.noContent().build();
    }
}
