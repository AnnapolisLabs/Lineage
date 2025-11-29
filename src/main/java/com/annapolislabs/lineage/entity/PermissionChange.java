package com.annapolislabs.lineage.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity for tracking permission changes and audit trail
 */
@Data
@Entity
@Table(name = "permission_changes", indexes = {
    @Index(name = "idx_permission_changes_user_id", columnList = "user_id"),
    @Index(name = "idx_permission_changes_changed_by", columnList = "changed_by"),
    @Index(name = "idx_permission_changes_permission_key", columnList = "permission_key"),
    @Index(name = "idx_permission_changes_resource_id", columnList = "resource_id"),
    @Index(name = "idx_permission_changes_change_type", columnList = "change_type"),
    @Index(name = "idx_permission_changes_effective_from", columnList = "effective_from"),
    @Index(name = "idx_permission_changes_effective_until", columnList = "effective_until")
})
@EntityListeners(AuditingEntityListener.class)
public class PermissionChange {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "changed_by", nullable = false)
    private UUID changedBy;

    @Column(name = "permission_key", nullable = false, length = 100)
    private String permissionKey;

    @Type(io.hypersistence.utils.hibernate.type.json.JsonType.class)
    @Column(name = "old_value", columnDefinition = "jsonb")
    private Map<String, Object> oldValue;

    @Type(io.hypersistence.utils.hibernate.type.json.JsonType.class)
    @Column(name = "new_value", columnDefinition = "jsonb")
    private Map<String, Object> newValue;

    @Column(name = "resource_id")
    private UUID resourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", length = 20, nullable = false)
    private ChangeType changeType = ChangeType.GRANT;

    @Column(length = 500)
    private String reason;

    @Column(nullable = false)
    private boolean approved = true;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "effective_from", nullable = false)
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_until")
    private LocalDateTime effectiveUntil;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "is_temporary")
    private boolean temporary = false;

    @Column(name = "auto_expire")
    private boolean autoExpire = false;

    @Type(io.hypersistence.utils.hibernate.type.json.JsonType.class)
    @Column(name = "context_data", columnDefinition = "jsonb")
    private Map<String, Object> contextData;

    @Column(length = 100)
    private String requestSource; // e.g., "api", "admin_panel", "system"

    @Column(length = 50)
    private String riskLevel; // LOW, MEDIUM, HIGH, CRITICAL

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum ChangeType {
        GRANT("grant", "Permission was granted"),
        REVOKE("revoke", "Permission was revoked"),
        MODIFY("modify", "Permission was modified"),
        EXTEND("extend", "Permission expiry was extended"),
        SUSPEND("suspend", "Permission was temporarily suspended");

        private final String value;
        private final String description;

        ChangeType(String value, String description) {
            this.value = value;
            this.description = description;
        }

        public String getValue() { return value; }
        public String getDescription() { return description; }
    }

    // Constructors
    public PermissionChange() {}

    public PermissionChange(UUID userId, UUID changedBy, String permissionKey, ChangeType changeType) {
        this.userId = userId;
        this.changedBy = changedBy;
        this.permissionKey = permissionKey;
        this.changeType = changeType;
        this.effectiveFrom = LocalDateTime.now();
    }

    // Business methods
    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return approved && 
               (effectiveFrom == null || !effectiveFrom.isAfter(now)) &&
               (effectiveUntil == null || !effectiveUntil.isBefore(now));
    }

    public boolean isExpired() {
        if (!temporary) return false;
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean needsApproval() {
        return !approved && effectiveFrom.isAfter(LocalDateTime.now());
    }

    public boolean isHighRisk() {
        return "HIGH".equals(riskLevel) || "CRITICAL".equals(riskLevel);
    }

    public void approve(UUID approverId) {
        this.approved = true;
        this.approvedBy = approverId;
        this.approvedAt = LocalDateTime.now();
    }

    public void reject() {
        this.approved = false;
        this.effectiveUntil = LocalDateTime.now(); // Expire immediately
    }

    public void setExpiration(LocalDateTime expirationDate, boolean autoExpire) {
        this.expiresAt = expirationDate;
        this.autoExpire = autoExpire;
        if (expirationDate != null && effectiveUntil == null) {
            this.effectiveUntil = expirationDate;
        }
    }

    public void revoke(String reason) {
        this.changeType = ChangeType.REVOKE;
        this.reason = reason;
        this.effectiveUntil = LocalDateTime.now();
    }

    public void extend(LocalDateTime newExpiration) {
        this.changeType = ChangeType.EXTEND;
        this.effectiveUntil = newExpiration;
        this.expiresAt = newExpiration;
    }

    public void suspend(String reason) {
        this.changeType = ChangeType.SUSPEND;
        this.reason = reason;
        // Keep the original effective from but set a new effective until
        if (effectiveUntil == null || effectiveUntil.isAfter(LocalDateTime.now().plusDays(30))) {
            this.effectiveUntil = LocalDateTime.now().plusDays(7); // Default 7-day suspension
        }
    }

    public void addContext(String key, Object value) {
        if (this.contextData == null) {
            this.contextData = new java.util.HashMap<>();
        }
        this.contextData.put(key, value);
    }

    public void removeContext(String key) {
        if (this.contextData != null) {
            this.contextData.remove(key);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PermissionChange that = (PermissionChange) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "PermissionChange{" +
                "id=" + id +
                ", userId=" + userId +
                ", changedBy=" + changedBy +
                ", permissionKey='" + permissionKey + '\'' +
                ", changeType=" + changeType +
                ", approved=" + approved +
                ", effectiveFrom=" + effectiveFrom +
                ", effectiveUntil=" + effectiveUntil +
                '}';
    }
}