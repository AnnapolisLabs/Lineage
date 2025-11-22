package com.annapolislabs.lineage.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImportRequirementRequest {

    @NotBlank(message = "Requirement ID is required")
    @Size(max = 200, message = "Requirement ID cannot exceed 200 characters")
    private String reqId;

    @NotBlank(message = "Title is required")
    @Size(max = 500, message = "Title must not exceed 500 characters")
    private String title;

    @Size(max = 50000, message = "Description must not exceed 50000 characters")
    private String description;

    private String status = "DRAFT";

    private String priority = "MEDIUM";

    /**
     * Parent requirement identifier (reqId) if applicable.
     */
    private String parentId;

    private Map<String, Object> customFields = new HashMap<>();

    /**
     * Optional historical timestamp preserved from exported data.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    @JsonProperty("createdAt")
    private LocalDateTime importedCreatedAt;
}
