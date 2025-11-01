package com.annapolislabs.lineage.dto.response;

import com.annapolislabs.lineage.entity.Project;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ProjectResponse {
    private UUID id;
    private String name;
    private String description;
    private String projectKey;
    private Map<String, String> levelPrefixes = new HashMap<>();
    private String createdByName;
    private String createdByEmail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ProjectResponse() {}

    public ProjectResponse(Project project) {
        this.id = project.getId();
        this.name = project.getName();
        this.description = project.getDescription();
        this.projectKey = project.getProjectKey();
        this.levelPrefixes = project.getLevelPrefixes();
        if (project.getCreatedBy() != null) {
            this.createdByName = project.getCreatedBy().getName();
            this.createdByEmail = project.getCreatedBy().getEmail();
        }
        this.createdAt = project.getCreatedAt();
        this.updatedAt = project.getUpdatedAt();
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProjectKey() {
        return projectKey;
    }

    public void setProjectKey(String projectKey) {
        this.projectKey = projectKey;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public String getCreatedByEmail() {
        return createdByEmail;
    }

    public void setCreatedByEmail(String createdByEmail) {
        this.createdByEmail = createdByEmail;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Map<String, String> getLevelPrefixes() {
        return levelPrefixes;
    }

    public void setLevelPrefixes(Map<String, String> levelPrefixes) {
        this.levelPrefixes = levelPrefixes;
    }
}
