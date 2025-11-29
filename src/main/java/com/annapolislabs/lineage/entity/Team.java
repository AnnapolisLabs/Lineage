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
 * Entity representing a team for collaboration and project organization
 */
@Data
@Entity
@Table(name = "teams", indexes = {
    @Index(name = "idx_teams_project_id", columnList = "project_id"),
    @Index(name = "idx_teams_created_by", columnList = "created_by"),
    @Index(name = "idx_teams_active", columnList = "is_active")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_teams_project_name", columnNames = {"project_id", "name"})
})
@EntityListeners(AuditingEntityListener.class)
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Type(io.hypersistence.utils.hibernate.type.json.JsonType.class)
    @Column(name = "settings", columnDefinition = "jsonb")
    private Map<String, Object> settings;

    @Column(name = "auto_assign_reviewers")
    private boolean autoAssignReviewers = false;

    @Column(name = "require_peer_review")
    private boolean requirePeerReview = true;

    @Column(name = "max_members")
    private Integer maxMembers = 50;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public Team() {}

    public Team(String name, String description, UUID projectId, UUID createdBy) {
        this.name = name;
        this.description = description;
        this.projectId = projectId;
        this.createdBy = createdBy;
    }

    // Business methods
    public boolean isAtCapacity() {
        return maxMembers != null && maxMembers <= getMemberCount();
    }

    public int getMemberCount() {
        // This would be calculated via a service or query
        // For now, return 0 as placeholder
        return 0;
    }

    public boolean canAddMember() {
        return active && (!isAtCapacity());
    }

    public boolean requiresPeerReview() {
        return requirePeerReview;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Team team = (Team) o;
        return Objects.equals(id, team.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Team{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", projectId=" + projectId +
                ", active=" + active +
                ", createdAt=" + createdAt +
                '}';
    }
}