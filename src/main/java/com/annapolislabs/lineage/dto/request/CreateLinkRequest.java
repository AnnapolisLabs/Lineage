package com.annapolislabs.lineage.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class CreateLinkRequest {

    @NotNull(message = "Target requirement ID is required")
    private UUID toRequirementId;

    public CreateLinkRequest() {}

    public CreateLinkRequest(UUID toRequirementId) {
        this.toRequirementId = toRequirementId;
    }

    public UUID getToRequirementId() {
        return toRequirementId;
    }

    public void setToRequirementId(UUID toRequirementId) {
        this.toRequirementId = toRequirementId;
    }
}
