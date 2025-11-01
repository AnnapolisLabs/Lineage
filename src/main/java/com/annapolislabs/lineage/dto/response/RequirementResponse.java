package com.annapolislabs.lineage.dto.response;

import com.annapolislabs.lineage.entity.Requirement;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

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

    public RequirementResponse() {}

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

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getReqId() {
        return reqId;
    }

    public void setReqId(String reqId) {
        this.reqId = reqId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public UUID getParentId() {
        return parentId;
    }

    public void setParentId(UUID parentId) {
        this.parentId = parentId;
    }

    public String getParentReqId() {
        return parentReqId;
    }

    public void setParentReqId(String parentReqId) {
        this.parentReqId = parentReqId;
    }

    public Map<String, Object> getCustomFields() {
        return customFields;
    }

    public void setCustomFields(Map<String, Object> customFields) {
        this.customFields = customFields;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public String getCreatedByEmail() {
        return createdByEmail;
    }

    public void setCreatedByEmail(String createdByEmail) {
        this.createdByEmail = createdByEmail;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public Integer getInLinkCount() {
        return inLinkCount;
    }

    public void setInLinkCount(Integer inLinkCount) {
        this.inLinkCount = inLinkCount;
    }

    public Integer getOutLinkCount() {
        return outLinkCount;
    }

    public void setOutLinkCount(Integer outLinkCount) {
        this.outLinkCount = outLinkCount;
    }
}
