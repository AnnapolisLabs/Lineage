package com.annapolislabs.lineage.controller;

import com.annapolislabs.lineage.dto.request.AIMessageRequest;
import com.annapolislabs.lineage.dto.response.AIChatListResponse;
import com.annapolislabs.lineage.dto.response.AIMessageResponse;
import com.annapolislabs.lineage.entity.AIConversation;
import com.annapolislabs.lineage.entity.User;
import com.annapolislabs.lineage.entity.UserRole;
import com.annapolislabs.lineage.service.AIAgentService;
import com.annapolislabs.lineage.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AIAgentControllerTest {

    @Mock
    private AIAgentService aiAgentService;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AIAgentController aiAgentController;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("test@example.com", "hashedPassword", "Test User", UserRole.DEVELOPER);
        testUser.setId(UUID.randomUUID());
    }

    @Test
    void sendMessage_WithExistingChatId_Success() {
        // Arrange
        AIMessageRequest request = new AIMessageRequest();
        request.setChatId("chat_123");
        request.setMessage("Hello AI");
        request.setProjectId(UUID.randomUUID().toString());

        when(authService.getCurrentUser()).thenReturn(testUser);
        when(aiAgentService.processMessage(eq("chat_123"), eq("Hello AI"), anyString(), eq(testUser)))
                .thenReturn("AI Response");

        // Act
        ResponseEntity<AIMessageResponse> response = aiAgentController.sendMessage(request);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("AI Response", response.getBody().getMessage());
        assertEquals("chat_123", response.getBody().getChatId());
        assertNull(response.getBody().getError());
    }

    @Test
    void sendMessage_WithoutChatId_CreatesNewConversation() {
        // Arrange
        AIMessageRequest request = new AIMessageRequest();
        request.setMessage("Hello AI");
        request.setProjectId(UUID.randomUUID().toString());

        when(authService.getCurrentUser()).thenReturn(testUser);
        when(aiAgentService.createNewConversation(testUser)).thenReturn("chat_456");
        when(aiAgentService.processMessage(eq("chat_456"), eq("Hello AI"), anyString(), eq(testUser)))
                .thenReturn("AI Response");

        // Act
        ResponseEntity<AIMessageResponse> response = aiAgentController.sendMessage(request);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("AI Response", response.getBody().getMessage());
        assertEquals("chat_456", response.getBody().getChatId());
        verify(aiAgentService).createNewConversation(testUser);
    }

    @Test
    void sendMessage_WithError_ReturnsErrorResponse() {
        // Arrange
        AIMessageRequest request = new AIMessageRequest();
        request.setChatId("chat_123");
        request.setMessage("Hello AI");
        request.setProjectId(UUID.randomUUID().toString());

        when(authService.getCurrentUser()).thenReturn(testUser);
        when(aiAgentService.processMessage(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Processing error"));

        // Act
        ResponseEntity<AIMessageResponse> response = aiAgentController.sendMessage(request);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNull(response.getBody().getMessage());
        assertNotNull(response.getBody().getError());
        assertTrue(response.getBody().getError().contains("Processing error"));
    }

    @Test
    void createNewChat_Success() {
        // Arrange
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(aiAgentService.createNewConversation(testUser)).thenReturn("chat_789");

        // Act
        ResponseEntity<String> response = aiAgentController.createNewChat();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("chat_789", response.getBody());
        verify(aiAgentService).createNewConversation(testUser);
    }

    @Test
    void getChatHistory_Success() {
        // Arrange
        AIConversation conv1 = new AIConversation("chat_1", testUser, "Chat 1");
        conv1.setUpdatedAt(LocalDateTime.now());
        AIConversation conv2 = new AIConversation("chat_2", testUser, "Chat 2");
        conv2.setUpdatedAt(LocalDateTime.now());

        when(authService.getCurrentUser()).thenReturn(testUser);
        when(aiAgentService.getConversationHistory(testUser)).thenReturn(Arrays.asList(conv1, conv2));

        // Act
        ResponseEntity<List<AIChatListResponse>> response = aiAgentController.getChatHistory();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        verify(aiAgentService).getConversationHistory(testUser);
    }

    @Test
    void deleteChat_Success() {
        // Arrange
        String chatId = "chat_123";
        when(authService.getCurrentUser()).thenReturn(testUser);
        doNothing().when(aiAgentService).deleteConversation(chatId, testUser);

        // Act
        ResponseEntity<Void> response = aiAgentController.deleteChat(chatId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(aiAgentService).deleteConversation(chatId, testUser);
    }
}
