package com.annapolislabs.lineage.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImportProjectMetadata {

    @NotBlank(message = "Project name is required")
    @Size(max = 255, message = "Project name must not exceed 255 characters")
    private String name;

    @Size(max = 1000, message = "Project description must not exceed 1000 characters")
    private String description;

    @NotBlank(message = "Project key is required")
    @Size(max = 50, message = "Project key must not exceed 50 characters")
    private String key;

    private Map<String, String> levelPrefixes = new HashMap<>();
}
