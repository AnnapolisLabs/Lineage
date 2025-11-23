package com.annapolislabs.lineage.dto.request;

import com.annapolislabs.lineage.entity.ProjectRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class CreateInvitationRequest {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
    
    @NotNull(message = "Project ID is required")
    private UUID projectId;
    
    @NotNull(message = "Project role is required")
    private ProjectRole projectRole;
    
    @Size(max = 500, message = "Message must not exceed 500 characters")
    private String message;
    
    // Getters and Setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }
    
    public ProjectRole getProjectRole() { return projectRole; }
    public void setProjectRole(ProjectRole projectRole) { this.projectRole = projectRole; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}