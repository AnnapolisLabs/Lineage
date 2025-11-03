package com.annapolislabs.lineage.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
public class UpdateRequirementRequest {

    @Size(max = 500, message = "Title must not exceed 500 characters")
    private String title;

    @Size(max = 50000, message = "Description must not exceed 50000 characters")
    private String description;

    private String status;

    private String priority;

    private UUID parentId;

    private Map<String, Object> customFields = new HashMap<>();
}
