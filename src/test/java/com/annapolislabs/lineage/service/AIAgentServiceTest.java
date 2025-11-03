package com.annapolislabs.lineage.service;

import com.annapolislabs.lineage.entity.AIConversation;
import com.annapolislabs.lineage.entity.User;
import com.annapolislabs.lineage.entity.UserRole;
import com.annapolislabs.lineage.mcp.McpTool;
import com.annapolislabs.lineage.repository.AIConversationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpClient;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AIAgentServiceTest {

    @Mock
    private AIConversationRepository conversationRepository;

    @Mock
    private List<McpTool> mcpTools;

    @Mock
    private HttpClient httpClient;

    @InjectMocks
    private AIAgentService aiAgentService;

    private User testUser;
    private AIConversation testConversation;

    @BeforeEach
    void setUp() {
        testUser = new User("test@example.com", "hashedPassword", "Test User", UserRole.EDITOR);
        testUser.setId(UUID.randomUUID());

        testConversation = new AIConversation("chat_123", testUser, "Test Chat");
        testConversation.setId(UUID.randomUUID());
    }

    @Test
    void createNewConversation_Success() {
        // Arrange
        when(conversationRepository.save(any(AIConversation.class))).thenReturn(testConversation);

        // Act
        String chatId = aiAgentService.createNewConversation(testUser);

        // Assert
        assertNotNull(chatId);
        assertTrue(chatId.startsWith("chat_"));
        verify(conversationRepository).save(any(AIConversation.class));
    }

    @Test
    void getConversationHistory_Success() {
        // Arrange
        List<AIConversation> conversations = Arrays.asList(testConversation);
        when(conversationRepository.findByUserOrderByUpdatedAtDesc(testUser)).thenReturn(conversations);

        // Act
        List<AIConversation> result = aiAgentService.getConversationHistory(testUser);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("chat_123", result.get(0).getChatId());
        verify(conversationRepository).findByUserOrderByUpdatedAtDesc(testUser);
    }

    @Test
    void getConversationHistory_LimitsTo20() {
        // Arrange
        List<AIConversation> conversations = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            conversations.add(new AIConversation("chat_" + i, testUser, "Chat " + i));
        }
        when(conversationRepository.findByUserOrderByUpdatedAtDesc(testUser)).thenReturn(conversations);

        // Act
        List<AIConversation> result = aiAgentService.getConversationHistory(testUser);

        // Assert
        assertNotNull(result);
        assertEquals(20, result.size());
    }

    @Test
    void deleteConversation_Success() {
        // Arrange
        String chatId = "chat_123";
        doNothing().when(conversationRepository).deleteByChatIdAndUser(chatId, testUser);

        // Act
        aiAgentService.deleteConversation(chatId, testUser);

        // Assert
        verify(conversationRepository).deleteByChatIdAndUser(chatId, testUser);
    }



    @Test
    void getConversationHistory_EmptyList() {
        // Arrange
        when(conversationRepository.findByUserOrderByUpdatedAtDesc(testUser))
                .thenReturn(new ArrayList<>());

        // Act
        List<AIConversation> result = aiAgentService.getConversationHistory(testUser);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
