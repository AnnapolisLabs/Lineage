package com.annapolislabs.lineage.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "user_roles", indexes = {
    @Index(name = "idx_user_roles_user_id", columnList = "user_id"),
    @Index(name = "idx_user_roles_role_id", columnList = "role_id"),
    @Index(name = "idx_user_roles_project_id", columnList = "project_id"),
    @Index(name = "idx_user_roles_scope", columnList = "scope")
})
@EntityListeners(AuditingEntityListener.class)
public class UserRoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @Column(name = "user_id", nullable = false, insertable = false, updatable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
    private User user;

    @NotNull
    @Column(name = "role_id", nullable = false, insertable = false, updatable = false)
    private UUID roleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Role role;

    // Project-specific role assignment
    @Column(name = "project_id", insertable = false, updatable = false)
    private UUID projectId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", referencedColumnName = "id")
    private Project project;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoleScope scope = RoleScope.GLOBAL;

    @Column(name = "granted_by", insertable = false, updatable = false)
    private UUID grantedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "granted_by", referencedColumnName = "id")
    private User grantedByUser;

    @Column(name = "granted_at", columnDefinition = "timestamp")
    private LocalDateTime grantedAt;

    @Column(name = "expires_at", columnDefinition = "timestamp")
    private LocalDateTime expiresAt;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Constructors
    public UserRoleEntity() {}

    public UserRoleEntity(UUID userId, UUID roleId, RoleScope scope) {
        this.userId = userId;
        this.roleId = roleId;
        this.scope = scope;
        this.grantedAt = LocalDateTime.now();
    }

    public UserRoleEntity(UUID userId, UUID roleId, UUID projectId, RoleScope scope) {
        this(userId, roleId, scope);
        this.projectId = projectId;
    }

    // Utility Methods
    public boolean isGlobalScope() {
        return scope == RoleScope.GLOBAL;
    }

    public boolean isProjectScope() {
        return scope == RoleScope.PROJECT;
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isActive() {
        return isActive && !isExpired();
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void extendExpiration(LocalDateTime newExpiration) {
        this.expiresAt = newExpiration;
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

    public UUID getRoleId() {
        return roleId;
    }

    public void setRoleId(UUID roleId) {
        this.roleId = roleId;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public RoleScope getScope() {
        return scope;
    }

    public void setScope(RoleScope scope) {
        this.scope = scope;
    }

    public UUID getGrantedBy() {
        return grantedBy;
    }

    public void setGrantedBy(UUID grantedBy) {
        this.grantedBy = grantedBy;
    }

    public User getGrantedByUser() {
        return grantedByUser;
    }

    public void setGrantedByUser(User grantedByUser) {
        this.grantedByUser = grantedByUser;
    }

    public LocalDateTime getGrantedAt() {
        return grantedAt;
    }

    public void setGrantedAt(LocalDateTime grantedAt) {
        this.grantedAt = grantedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(boolean active) {
        isActive = active;
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
        UserRoleEntity userRole = (UserRoleEntity) o;
        return Objects.equals(id, userRole.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "UserRoleEntity{" +
                "id=" + id +
                ", userId=" + userId +
                ", roleId=" + roleId +
                ", projectId=" + projectId +
                ", scope=" + scope +
                ", grantedBy=" + grantedBy +
                ", grantedAt=" + grantedAt +
                ", expiresAt=" + expiresAt +
                ", isActive=" + isActive +
                ", createdAt=" + createdAt +
                '}';
    }
}