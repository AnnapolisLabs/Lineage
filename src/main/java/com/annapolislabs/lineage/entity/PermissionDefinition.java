package com.annapolislabs.lineage.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing a permission definition in the RBAC system
 * Defines available permissions that can be granted to users and roles
 */
@Data
@Entity
@Table(name = "permission_definitions", indexes = {
    @Index(name = "idx_permission_definitions_resource", columnList = "resource"),
    @Index(name = "idx_permission_definitions_category", columnList = "category"),
    @Index(name = "idx_permission_definitions_risk_level", columnList = "risk_level"),
    @Index(name = "idx_permission_definitions_key", columnList = "permission_key", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
public class PermissionDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "permission_key", nullable = false, unique = true, length = 100)
    private String permissionKey;

    @Column(nullable = false, length = 50)
    private String resource;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(length = 500)
    private String description;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "requires_confirmation")
    private boolean requiresConfirmation = false;

    @Column(name = "audit_required")
    private boolean auditRequired = true;

    @Column(name = "is_system")
    private boolean system = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", length = 20, nullable = false)
    private RiskLevel riskLevel = RiskLevel.LOW;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum RiskLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    // Constructors
    public PermissionDefinition() {}

    public PermissionDefinition(String permissionKey, String resource, String action, String description, String category) {
        this.permissionKey = permissionKey;
        this.resource = resource;
        this.action = action;
        this.description = description;
        this.category = category;
    }

    // Business methods
    public String getFullPermissionKey() {
        return resource + "." + action;
    }

    public boolean isHighRisk() {
        return riskLevel == RiskLevel.HIGH || riskLevel == RiskLevel.CRITICAL;
    }

    public boolean requiresAuditTrail() {
        return auditRequired || isHighRisk();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PermissionDefinition that = (PermissionDefinition) o;
        return Objects.equals(id, that.id) || Objects.equals(permissionKey, that.permissionKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, permissionKey);
    }

    @Override
    public String toString() {
        return "PermissionDefinition{" +
                "id=" + id +
                ", permissionKey='" + permissionKey + '\'' +
                ", resource='" + resource + '\'' +
                ", action='" + action + '\'' +
                ", category='" + category + '\'' +
                ", riskLevel=" + riskLevel +
                '}';
    }
}