package com.annapolislabs.lineage.dto.response;

import com.annapolislabs.lineage.entity.InvitationStatus;
import com.annapolislabs.lineage.entity.ProjectRole;

import java.time.LocalDateTime;
import java.util.UUID;

public class InvitationResponse {
    
    private UUID id;
    private UUID invitedBy;
    private String invitedByName;
    private String email;
    private UUID projectId;
    private String projectName;
    private ProjectRole projectRole;
    private String token;
    private InvitationStatus status;
    private LocalDateTime expiryDate;
    private LocalDateTime acceptedAt;
    private String message;
    private LocalDateTime createdAt;
    
    // Constructors
    public InvitationResponse() {}
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public UUID getInvitedBy() { return invitedBy; }
    public void setInvitedBy(UUID invitedBy) { this.invitedBy = invitedBy; }
    
    public String getInvitedByName() { return invitedByName; }
    public void setInvitedByName(String invitedByName) { this.invitedByName = invitedByName; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }
    
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    
    public ProjectRole getProjectRole() { return projectRole; }
    public void setProjectRole(ProjectRole projectRole) { this.projectRole = projectRole; }
    
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    
    public InvitationStatus getStatus() { return status; }
    public void setStatus(InvitationStatus status) { this.status = status; }
    
    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }
    
    public LocalDateTime getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(LocalDateTime acceptedAt) { this.acceptedAt = acceptedAt; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}