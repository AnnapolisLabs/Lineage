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
 * Entity representing a team membership
 */
@Data
@Entity
@Table(name = "team_members", indexes = {
    @Index(name = "idx_team_members_team_id", columnList = "team_id"),
    @Index(name = "idx_team_members_user_id", columnList = "user_id"),
    @Index(name = "idx_team_members_status", columnList = "status")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_team_members_team_user", columnNames = {"team_id", "user_id"})
})
@EntityListeners(AuditingEntityListener.class)
public class TeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * Transient reference to the associated user.
     *
     * This field is not persisted in the team_members table – it is
     * populated on demand in service layer methods (e.g. when returning
     * team members via the API) so that API consumers can access
     * denormalised user details alongside membership data.
     */
    @Transient
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TeamRole role = TeamRole.MEMBER;

    @Type(io.hypersistence.utils.hibernate.type.json.JsonType.class)
    @Column(name = "permissions", columnDefinition = "jsonb")
    private Map<String, Object> permissions;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "invited_by")
    private UUID invitedBy;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private TeamMemberStatus status = TeamMemberStatus.ACTIVE;

    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    @Column(name = "contribution_score")
    private Integer contributionScore = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum TeamRole {
        OWNER("owner", "Team Owner", 4),
        ADMIN("admin", "Team Administrator", 3),
        MEMBER("member", "Team Member", 2),
        VIEWER("viewer", "Team Viewer", 1);

        private final String value;
        private final String displayName;
        private final int level;

        TeamRole(String value, String displayName, int level) {
            this.value = value;
            this.displayName = displayName;
            this.level = level;
        }

        public String getValue() { return value; }
        public String getDisplayName() { return displayName; }
        public int getLevel() { return level; }

        public boolean hasPermissionLevelOrHigher(TeamRole other) {
            return this.level >= other.level;
        }
    }

    public enum TeamMemberStatus {
        ACTIVE, INACTIVE, PENDING
    }

    // Constructors
    public TeamMember() {}

    public TeamMember(UUID teamId, UUID userId, TeamRole role) {
        this.teamId = teamId;
        this.userId = userId;
        this.role = role;
        this.joinedAt = LocalDateTime.now();
    }

    // Business methods
    public boolean isActive() {
        return status == TeamMemberStatus.ACTIVE;
    }

    public boolean canManage() {
        return role == TeamRole.OWNER || role == TeamRole.ADMIN;
    }

    public boolean canEdit() {
        return role == TeamRole.OWNER || role == TeamRole.ADMIN || role == TeamRole.MEMBER;
    }

    public boolean canInvite() {
        return role == TeamRole.OWNER || role == TeamRole.ADMIN;
    }

    public boolean canRemove(TeamMember other) {
        if (!this.canManage()) return false;
        if (this.role == TeamRole.OWNER && other.role == TeamRole.OWNER) return false;
        return this.role.hasPermissionLevelOrHigher(other.role);
    }

    public void activate() {
        this.status = TeamMemberStatus.ACTIVE;
        this.lastActivityAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.status = TeamMemberStatus.INACTIVE;
    }

    public void promote(TeamRole newRole) {
        this.role = newRole;
    }

    public void demote(TeamRole newRole) {
        this.role = newRole;
    }

    public void updateActivity() {
        this.lastActivityAt = LocalDateTime.now();
    }

    public void incrementContributionScore(int score) {
        this.contributionScore = (this.contributionScore != null ? this.contributionScore : 0) + score;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TeamMember that = (TeamMember) o;
        return Objects.equals(id, that.id) ||
               (Objects.equals(teamId, that.teamId) && Objects.equals(userId, that.userId));
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, teamId, userId);
    }

    @Override
    public String toString() {
        return "TeamMember{" +
                "id=" + id +
                ", teamId=" + teamId +
                ", userId=" + userId +
                ", role=" + role +
                ", status=" + status +
                ", joinedAt=" + joinedAt +
                '}';
    }
}