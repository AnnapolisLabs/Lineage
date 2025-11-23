package com.annapolislabs.lineage.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity for storing AI agent conversation history
 */
@Setter
@Getter
@Entity
@Table(name = "ai_conversations")
@EntityListeners(AuditingEntityListener.class)
public class AIConversation {

    // Getters and Setters
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private String chatId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false, length = 500)
    private String title;
    
    /**
     * Conversation messages stored as JSON array
     * Format: [{"role": "user|assistant|system", "content": "..."}]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<ConversationMessage> messages = new ArrayList<>();
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    // Constructors
    public AIConversation() {}
    
    public AIConversation(String chatId, User user, String title) {
        this.chatId = chatId;
        this.user = user;
        this.title = title;
    }

    /**
     * Inner class for conversation messages
     */
    public static class ConversationMessage {
        private String role; // "user", "assistant", "system"
        private String content;
        
        public ConversationMessage() {}
        
        public ConversationMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
        
        public String getRole() {
            return role;
        }
        
        public void setRole(String role) {
            this.role = role;
        }
        
        public String getContent() {
            return content;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
    }
}
