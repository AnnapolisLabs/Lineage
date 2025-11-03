package com.annapolislabs.lineage.dto.response;

/**
 * Response DTO for AI agent message
 */
public class AIMessageResponse {

    private String message;
    private String chatId;
    private boolean isProcessing;
    private String error;

    public AIMessageResponse() {}

    public AIMessageResponse(String message, String chatId, boolean isProcessing, String error) {
        this.message = message;
        this.chatId = chatId;
        this.isProcessing = isProcessing;
        this.error = error;
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

    public boolean isProcessing() {
        return isProcessing;
    }

    public void setProcessing(boolean processing) {
        isProcessing = processing;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
