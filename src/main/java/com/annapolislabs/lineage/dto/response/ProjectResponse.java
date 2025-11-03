package com.annapolislabs.lineage.dto.response;

import com.annapolislabs.lineage.entity.Project;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
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
}
