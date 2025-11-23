package com.annapolislabs.lineage.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImportProjectRequest {

    @Valid
    @NotNull(message = "Project metadata is required")
    private ImportProjectMetadata project;

    @Valid
    private List<ImportRequirementRequest> requirements = new ArrayList<>();
}
