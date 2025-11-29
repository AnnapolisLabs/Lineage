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
 * Entity representing a peer review for requirements and deliverables
 */
@Data
@Entity
@Table(name = "peer_reviews", indexes = {
    @Index(name = "idx_peer_reviews_requirement_id", columnList = "requirement_id"),
    @Index(name = "idx_peer_reviews_reviewer_id", columnList = "reviewer_id"),
    @Index(name = "idx_peer_reviews_author_id", columnList = "author_id"),
    @Index(name = "idx_peer_reviews_status", columnList = "status"),
    @Index(name = "idx_peer_reviews_reviewed_at", columnList = "reviewed_at")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_peer_reviews_requirement_reviewer", columnNames = {"requirement_id", "reviewer_id"})
})
@EntityListeners(AuditingEntityListener.class)
public class PeerReview {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "requirement_id", nullable = false)
    private UUID requirementId;

    @Column(name = "reviewer_id", nullable = false)
    private UUID reviewerId;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private ReviewStatus status = ReviewStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_type", length = 50)
    private ReviewType reviewType = ReviewType.CODE;

    @Column(length = 2000)
    private String comments;

    @Type(io.hypersistence.utils.hibernate.type.json.JsonType.class)
    @Column(name = "review_details", columnDefinition = "jsonb")
    private Map<String, Object> reviewDetails;

    @Column(name = "effort_rating")
    private Integer effortRating; // 1-5 scale

    @Column(name = "quality_rating")
    private Integer qualityRating; // 1-5 scale

    @Column(name = "priority_suggestion")
    @Enumerated(EnumType.STRING)
    private PrioritySuggestion prioritySuggestion;

    @Column(name = "review_deadline")
    private LocalDateTime reviewDeadline;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "time_spent_minutes")
    private Integer timeSpentMinutes;

    @ElementCollection
    @CollectionTable(name = "review_files", joinColumns = @JoinColumn(name = "review_id"))
    @Column(name = "file_path")
    private java.util.List<String> reviewedFiles;

    @ElementCollection
    @CollectionTable(name = "review_tags", joinColumns = @JoinColumn(name = "review_id"))
    @Column(name = "tag")
    private java.util.List<String> tags;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum ReviewStatus {
        PENDING("pending", "Review is pending"),
        IN_PROGRESS("in_progress", "Review is in progress"),
        APPROVED("approved", "Review has been approved"),
        REJECTED("rejected", "Review has been rejected"),
        REVISION_REQUESTED("revision_requested", "Revision has been requested");

        private final String value;
        private final String description;

        ReviewStatus(String value, String description) {
            this.value = value;
            this.description = description;
        }

        public String getValue() { return value; }
        public String getDescription() { return description; }
    }

    public enum ReviewType {
        CODE("code", "Code Review"),
        DESIGN("design", "Design Review"),
        DOCUMENTATION("documentation", "Documentation Review"),
        PROCESS("process", "Process Review"),
        REQUIREMENTS("requirements", "Requirements Review");

        private final String value;
        private final String displayName;

        ReviewType(String value, String displayName) {
            this.value = value;
            this.displayName = displayName;
        }

        public String getValue() { return value; }
        public String getDisplayName() { return displayName; }
    }

    public enum PrioritySuggestion {
        MAINTAIN("maintain", "Maintain current priority"),
        INCREASE("increase", "Increase priority"),
        DECREASE("decrease", "Decrease priority");

        private final String value;
        private final String description;

        PrioritySuggestion(String value, String description) {
            this.value = value;
            this.description = description;
        }

        public String getValue() { return value; }
        public String getDescription() { return description; }
    }

    // Constructors
    public PeerReview() {}

    public PeerReview(UUID requirementId, UUID reviewerId, UUID authorId, ReviewType reviewType) {
        this.requirementId = requirementId;
        this.reviewerId = reviewerId;
        this.authorId = authorId;
        this.reviewType = reviewType;
        this.status = ReviewStatus.PENDING;
    }

    // Business methods
    public boolean isPending() {
        return status == ReviewStatus.PENDING;
    }

    public boolean isCompleted() {
        return status == ReviewStatus.APPROVED || 
               status == ReviewStatus.REJECTED || 
               status == ReviewStatus.REVISION_REQUESTED;
    }

    public boolean isOverdue() {
        return reviewDeadline != null && 
               LocalDateTime.now().isAfter(reviewDeadline) && 
               !isCompleted();
    }

    public boolean canEdit() {
        return status == ReviewStatus.PENDING || status == ReviewStatus.IN_PROGRESS;
    }

    public boolean canComplete() {
        return status == ReviewStatus.IN_PROGRESS || status == ReviewStatus.PENDING;
    }

    public void startReview() {
        if (status == ReviewStatus.PENDING) {
            this.status = ReviewStatus.IN_PROGRESS;
        }
    }

    public void approve(String comments) {
        if (canComplete()) {
            this.status = ReviewStatus.APPROVED;
            this.comments = comments;
            this.reviewedAt = LocalDateTime.now();
        }
    }

    public void reject(String comments) {
        if (canComplete()) {
            this.status = ReviewStatus.REJECTED;
            this.comments = comments;
            this.reviewedAt = LocalDateTime.now();
        }
    }

    public void requestRevision(String comments) {
        if (canComplete()) {
            this.status = ReviewStatus.REVISION_REQUESTED;
            this.comments = comments;
            this.reviewedAt = LocalDateTime.now();
        }
    }

    public void setRatings(Integer effortRating, Integer qualityRating) {
        if (effortRating != null && (effortRating < 1 || effortRating > 5)) {
            throw new IllegalArgumentException("Effort rating must be between 1 and 5");
        }
        if (qualityRating != null && (qualityRating < 1 || qualityRating > 5)) {
            throw new IllegalArgumentException("Quality rating must be between 1 and 5");
        }
        this.effortRating = effortRating;
        this.qualityRating = qualityRating;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.reviewDeadline = deadline;
    }

    public void addReviewedFile(String filePath) {
        if (this.reviewedFiles == null) {
            this.reviewedFiles = new java.util.ArrayList<>();
        }
        if (!this.reviewedFiles.contains(filePath)) {
            this.reviewedFiles.add(filePath);
        }
    }

    public void removeReviewedFile(String filePath) {
        if (this.reviewedFiles != null) {
            this.reviewedFiles.remove(filePath);
        }
    }

    public void addTag(String tag) {
        if (this.tags == null) {
            this.tags = new java.util.ArrayList<>();
        }
        if (!this.tags.contains(tag)) {
            this.tags.add(tag);
        }
    }

    public void removeTag(String tag) {
        if (this.tags != null) {
            this.tags.remove(tag);
        }
    }

    public void setTimeSpent(int minutes) {
        this.timeSpentMinutes = Math.max(0, minutes);
    }

    public boolean hasQualityRating() {
        return qualityRating != null;
    }

    public boolean hasEffortRating() {
        return effortRating != null;
    }

    public double getAverageRating() {
        if (effortRating == null && qualityRating == null) return 0;
        if (effortRating == null) return qualityRating;
        if (qualityRating == null) return effortRating;
        return (effortRating + qualityRating) / 2.0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PeerReview peerReview = (PeerReview) o;
        return Objects.equals(id, peerReview.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "PeerReview{" +
                "id=" + id +
                ", requirementId=" + requirementId +
                ", reviewerId=" + reviewerId +
                ", authorId=" + authorId +
                ", status=" + status +
                ", reviewType=" + reviewType +
                ", reviewedAt=" + reviewedAt +
                '}';
    }
}