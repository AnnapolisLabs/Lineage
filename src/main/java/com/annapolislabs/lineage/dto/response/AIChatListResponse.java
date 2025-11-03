package com.annapolislabs.lineage.dto.response;

import java.time.LocalDateTime;

/**
 * Response DTO for AI chat list item
 */
public class AIChatListResponse {

    private String id;
    private String title;
    private LocalDateTime timestamp;
    private int messageCount;

    public AIChatListResponse() {}

    public AIChatListResponse(String id, String title, LocalDateTime timestamp, int messageCount) {
        this.id = id;
        this.title = title;
        this.timestamp = timestamp;
        this.messageCount = messageCount;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }
}
