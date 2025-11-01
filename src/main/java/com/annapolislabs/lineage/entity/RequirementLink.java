package com.annapolislabs.lineage.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "requirement_links",
       uniqueConstraints = @UniqueConstraint(columnNames = {"from_requirement_id", "to_requirement_id"}))
@EntityListeners(AuditingEntityListener.class)
public class RequirementLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_requirement_id", nullable = false)
    private Requirement fromRequirement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_requirement_id", nullable = false)
    private Requirement toRequirement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Constructors
    public RequirementLink() {}

    public RequirementLink(Requirement fromRequirement, Requirement toRequirement, User createdBy) {
        this.fromRequirement = fromRequirement;
        this.toRequirement = toRequirement;
        this.createdBy = createdBy;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Requirement getFromRequirement() {
        return fromRequirement;
    }

    public void setFromRequirement(Requirement fromRequirement) {
        this.fromRequirement = fromRequirement;
    }

    public Requirement getToRequirement() {
        return toRequirement;
    }

    public void setToRequirement(Requirement toRequirement) {
        this.toRequirement = toRequirement;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
