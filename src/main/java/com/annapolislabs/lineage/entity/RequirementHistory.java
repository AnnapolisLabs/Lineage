package com.annapolislabs.lineage.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Setter
@Getter
@Entity
@Table(name = "requirement_history")
@EntityListeners(AuditingEntityListener.class)
public class RequirementHistory {

    // Getters and Setters
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requirement_id", nullable = false)
    private Requirement requirement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    private User changedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChangeType changeType;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> oldValue;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> newValue;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime changedAt;

    // Constructors
    public RequirementHistory() {}

    public RequirementHistory(Requirement requirement, User changedBy, ChangeType changeType,
                             Map<String, Object> oldValue, Map<String, Object> newValue) {
        this.requirement = requirement;
        this.changedBy = changedBy;
        this.changeType = changeType;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

}
