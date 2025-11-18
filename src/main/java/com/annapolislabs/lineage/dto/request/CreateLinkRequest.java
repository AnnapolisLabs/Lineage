package com.annapolislabs.lineage.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
public class CreateLinkRequest {

    @NotNull(message = "Target requirement ID is required")
    private UUID toRequirementId;

    public CreateLinkRequest() {}

    public CreateLinkRequest(UUID toRequirementId) {
        this.toRequirementId = toRequirementId;
    }

}
