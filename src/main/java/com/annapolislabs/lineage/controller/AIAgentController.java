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
 * REST controller that exposes conversational AI chat endpoints for authenticated users.
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
     * POST /api/ai/chat processes a user prompt, ensuring a conversation exists and returning the AI reply.
     * Returns 200 OK with an {@link AIMessageResponse} body even when downstream errors occur.
     *
     * @param request validated payload containing the chat message, project context, and optional chatId
     * @return 200 OK with the generated response and selected chat identifier
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
     * POST /api/ai/chat/new initializes a blank conversation owned by the current user and returns its ID.
     *
     * @return 200 OK containing the new chatId
     */
    @PostMapping("/chat/new")
    public ResponseEntity<String> createNewChat() {
        User currentUser = authService.getCurrentUser();
        String chatId = aiAgentService.createNewConversation(currentUser);
        return ResponseEntity.ok(chatId);
    }
    
    /**
     * GET /api/ai/chat/history lists conversation metadata for the authenticated user to power history UIs.
     *
     * @return 200 OK with lightweight summaries of each chat owned by the caller
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
                .toList();
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * DELETE /api/ai/chat/{chatId} removes the specified conversation when owned by the caller.
     *
     * @param chatId identifier of the conversation to purge
     * @return 204 No Content on success
     */
    @DeleteMapping("/chat/{chatId}")
    public ResponseEntity<Void> deleteChat(@PathVariable String chatId) {
        User currentUser = authService.getCurrentUser();
        aiAgentService.deleteConversation(chatId, currentUser);
        return ResponseEntity.noContent().build();
    }
}
