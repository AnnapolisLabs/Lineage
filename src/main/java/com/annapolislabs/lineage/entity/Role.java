package com.annapolislabs.lineage.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Setter
@Getter
@Entity
@Table(name = "roles", indexes = {
    @Index(name = "idx_roles_name", columnList = "name", unique = true),
    @Index(name = "idx_roles_type", columnList = "type")
})
@EntityListeners(AuditingEntityListener.class)
public class Role {

    // Getters and Setters
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Size(max = 255)
    private String description;

    @NotBlank
    @Column(nullable = false, length = 20)
    private String type; // GLOBAL, PROJECT

    @Column(name = "is_system", nullable = false)
    private boolean isSystem = false;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "display_order")
    private Integer displayOrder;

    // JSONB for role permissions
    @Column(name = "permissions", columnDefinition = "jsonb")
    private String permissions; // JSON string for permissions

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Relationships
    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<UserRoleEntity> userRoles = new HashSet<>();

    // Constructors
    public Role() {}

    public Role(String name, String description, String type, boolean isSystem) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.isSystem = isSystem;
    }

    // Utility Methods
    public boolean isGlobalRole() {
        return "GLOBAL".equals(type);
    }

    public boolean isProjectRole() {
        return "PROJECT".equals(type);
    }

    public boolean hasPermission(String permission) {
        if (permissions == null || permissions.isBlank()) {
            return false;
        }
        // Simple permission check - in production, use a proper JSON parsing library
        return permissions.contains("\"" + permission + "\"") || permissions.contains(permission);
    }

    public void addPermission(String permission) {
        // Simple implementation - in production, use proper JSON handling
        if (permissions == null || permissions.isBlank()) {
            permissions = "[\"" + permission + "\"]";
        } else {
            permissions = permissions.substring(0, permissions.length() - 1) + ",\"" + permission + "\"]";
        }
    }

    public void removePermission(String permission) {
        if (permissions == null || permissions.isBlank()) {
            return;
        }
        // Simple implementation - in production, use proper JSON handling
        permissions = permissions.replace("\"" + permission + "\",", "")
                                .replace(",\"" + permission + "\"", "")
                                .replace("\"" + permission + "\"", "");
        
        // Clean up empty arrays
        if (permissions.equals("[]")) {
            permissions = null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return Objects.equals(id, role.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Role{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", type='" + type + '\'' +
                ", isSystem=" + isSystem +
                ", isActive=" + isActive +
                ", displayOrder=" + displayOrder +
                ", createdAt=" + createdAt +
                '}';
    }
}