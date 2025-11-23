package com.annapolislabs.lineage.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Response DTO for AI chat list item
 */
@Setter
@Getter
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

}
