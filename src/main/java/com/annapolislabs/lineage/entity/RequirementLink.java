package com.annapolislabs.lineage.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Setter
@Getter
@Entity
@Table(name = "requirement_links",
       uniqueConstraints = @UniqueConstraint(columnNames = {"from_requirement_id", "to_requirement_id"}))
@EntityListeners(AuditingEntityListener.class)
public class RequirementLink {

    // Getters and Setters
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

}
