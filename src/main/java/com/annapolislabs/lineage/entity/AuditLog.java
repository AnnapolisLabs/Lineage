package com.annapolislabs.lineage.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.hypersistence.utils.hibernate.type.json.JsonType;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_logs_user_id", columnList = "user_id"),
    @Index(name = "idx_audit_logs_action", columnList = "action"),
    @Index(name = "idx_audit_logs_resource", columnList = "resource"),
    @Index(name = "idx_audit_logs_resource_id", columnList = "resource_id"),
    @Index(name = "idx_audit_logs_severity", columnList = "severity"),
    @Index(name = "idx_audit_logs_created_at", columnList = "created_at"),
    @Index(name = "idx_audit_logs_ip_address", columnList = "ip_address")
})
@EntityListeners(AuditingEntityListener.class)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", insertable = false, updatable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String action;

    @Size(max = 100)
    @Column(length = 100)
    private String resource;

    @Size(max = 255)
    @Column(name = "resource_id", length = 255)
    private String resourceId;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> details;

    @Size(max = 45)
    @Column(name = "ip_address", length = 45)
    private String ipAddress; // IPv6 compatible

    @Column(name = "user_agent", columnDefinition = "text")
    private String userAgent;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuditSeverity severity = AuditSeverity.INFO;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Constructors
    public AuditLog() {}

    public AuditLog(UUID userId, String action, String resource, String resourceId, AuditSeverity severity) {
        this.userId = userId;
        this.action = action;
        this.resource = resource;
        this.resourceId = resourceId;
        this.severity = severity;
    }

    public AuditLog(UUID userId, String action, String resource, String resourceId, Map<String, Object> details, AuditSeverity severity) {
        this(userId, action, resource, resourceId, severity);
        this.details = details;
    }

    public AuditLog(UUID userId, String action, String resource, String resourceId, Map<String, Object> details, 
                   AuditSeverity severity, String ipAddress, String userAgent) {
        this(userId, action, resource, resourceId, details, severity);
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    // Utility Methods
    public boolean isSecurityEvent() {
        return severity == AuditSeverity.WARNING || 
               severity == AuditSeverity.ERROR || 
               severity == AuditSeverity.CRITICAL;
    }

    public boolean isCriticalEvent() {
        return severity == AuditSeverity.CRITICAL;
    }

    public String getEventDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(action);
        if (resource != null) {
            sb.append(" on ").append(resource);
            if (resourceId != null) {
                sb.append(" (").append(resourceId).append(")");
            }
        }
        return sb.toString();
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public AuditSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(AuditSeverity severity) {
        this.severity = severity;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuditLog auditLog = (AuditLog) o;
        return Objects.equals(id, auditLog.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "AuditLog{" +
                "id=" + id +
                ", userId=" + userId +
                ", action='" + action + '\'' +
                ", resource='" + resource + '\'' +
                ", resourceId='" + resourceId + '\'' +
                ", severity=" + severity +
                ", ipAddress='" + ipAddress + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}