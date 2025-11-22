package com.annapolislabs.lineage.service.dto;

import com.annapolislabs.lineage.dto.request.ImportRequirementRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ImportedRequirement {
    private final ImportRequirementRequest payload;
    private final String parentReqId;
}
