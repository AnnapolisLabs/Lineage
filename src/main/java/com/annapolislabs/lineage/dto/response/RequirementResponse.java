package com.annapolislabs.lineage.dto.response;

import com.annapolislabs.lineage.entity.Requirement;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
public class RequirementResponse {
    private UUID id;
    private String reqId;
    private String title;
    private String description;
    private String status;
    private String priority;
    private UUID parentId;
    private String parentReqId;
    private Integer level;
    private String section;
    private Map<String, Object> customFields;
    private String createdByName;
    private String createdByEmail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer inLinkCount;
    private Integer outLinkCount;

    public RequirementResponse(Requirement requirement) {
        this.id = requirement.getId();
        this.reqId = requirement.getReqId();
        this.title = requirement.getTitle();
        this.description = requirement.getDescription();
        this.status = requirement.getStatus();
        this.priority = requirement.getPriority();
        if (requirement.getParent() != null) {
            this.parentId = requirement.getParent().getId();
            this.parentReqId = requirement.getParent().getReqId();
        }
        this.level = requirement.getLevel();
        this.section = requirement.getSection();
        this.customFields = requirement.getCustomFields();
        if (requirement.getCreatedBy() != null) {
            this.createdByName = requirement.getCreatedBy().getName();
            this.createdByEmail = requirement.getCreatedBy().getEmail();
        }
        this.createdAt = requirement.getCreatedAt();
        this.updatedAt = requirement.getUpdatedAt();
    }
}
