package com.annapolislabs.lineage.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for sending a message to the AI agent
 */
public class AIMessageRequest {

    @NotBlank(message = "Message is required")
    private String message;

    private String chatId;

    private String projectId;

    public AIMessageRequest() {}

    public AIMessageRequest(String message, String chatId, String projectId) {
        this.message = message;
        this.chatId = chatId;
        this.projectId = projectId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }
}
