package com.annapolislabs.lineage.dto.response;

import com.annapolislabs.lineage.entity.AuditSeverity;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public class AuditLogResponse {
    
    private UUID id;
    private UUID userId;
    private String userEmail;
    private String action;
    private String resource;
    private String resourceId;
    private Map<String, Object> details;
    private String ipAddress;
    private String userAgent;
    private AuditSeverity severity;
    private LocalDateTime createdAt;
    
    // Constructors
    /**
     * Default constructor required for JSON deserialization.
     * Field values are set via setters after construction.
     */
    public AuditLogResponse() {}
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    
    public String getResource() { return resource; }
    public void setResource(String resource) { this.resource = resource; }
    
    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }
    
    public Map<String, Object> getDetails() { return details; }
    public void setDetails(Map<String, Object> details) { this.details = details; }
    
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    
    public AuditSeverity getSeverity() { return severity; }
    public void setSeverity(AuditSeverity severity) { this.severity = severity; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}