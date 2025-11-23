package com.annapolislabs.lineage.dto.response;

import lombok.Getter;
import lombok.Setter;

/**
 * Response DTO for AI agent message
 */
@Setter
@Getter
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

}
