package com.annapolislabs.lineage.service.dto;

import com.annapolislabs.lineage.dto.request.ImportRequirementRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * DTO used internally by {@link com.annapolislabs.lineage.service.ProjectImportService} to store the
 * original requirement payload alongside the referenced parent requirement ID. Separating the raw
 * payload from the parent lookup key simplifies cycle detection and graph construction during
 * imports.
 */
@Getter
@RequiredArgsConstructor
public class ImportedRequirement {
    /** Incoming requirement definition as submitted by clients. */
    private final ImportRequirementRequest payload;
    /** Parent requirement identifier from the import file; may be {@code null} for root nodes. */
    private final String parentReqId;
}
