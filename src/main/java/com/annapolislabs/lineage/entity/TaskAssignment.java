package com.annapolislabs.lineage.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing a task assignment for collaboration and project management
 */
@Data
@Entity
@Table(name = "task_assignments", indexes = {
    @Index(name = "idx_task_assignments_assigned_to", columnList = "assigned_to"),
    @Index(name = "idx_task_assignments_assigned_by", columnList = "assigned_by"),
    @Index(name = "idx_task_assignments_project_id", columnList = "project_id"),
    @Index(name = "idx_task_assignments_requirement_id", columnList = "requirement_id"),
    @Index(name = "idx_task_assignments_status", columnList = "status"),
    @Index(name = "idx_task_assignments_priority", columnList = "priority"),
    @Index(name = "idx_task_assignments_due_date", columnList = "due_date")
})
@EntityListeners(AuditingEntityListener.class)
public class TaskAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "task_title", nullable = false, length = 255)
    private String taskTitle;

    @Column(name = "task_description", length = 2000)
    private String taskDescription;

    @Column(name = "assigned_by", nullable = false)
    private UUID assignedBy;

    @Column(name = "assigned_to", nullable = false)
    private UUID assignedTo;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "requirement_id")
    private UUID requirementId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private TaskStatus status = TaskStatus.ASSIGNED;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private TaskPriority priority = TaskPriority.MEDIUM;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "estimated_hours")
    private Integer estimatedHours;

    @Column(name = "actual_hours")
    private Integer actualHours;

    @Column(length = 1000)
    private String notes;

    @Column(name = "completion_notes", length = 2000)
    private String completionNotes;

    @ElementCollection
    @CollectionTable(name = "task_tags", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "tag")
    private java.util.List<String> tags;

    @ElementCollection
    @CollectionTable(name = "task_blockers", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "blocker")
    private java.util.List<String> blockers;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum TaskStatus {
        ASSIGNED("assigned", "Task has been assigned"),
        IN_PROGRESS("in_progress", "Task is being worked on"),
        COMPLETED("completed", "Task has been completed"),
        CANCELLED("cancelled", "Task has been cancelled");

        private final String value;
        private final String description;

        TaskStatus(String value, String description) {
            this.value = value;
            this.description = description;
        }

        public String getValue() { return value; }
        public String getDescription() { return description; }

        public boolean canTransitionTo(TaskStatus newStatus) {
            switch (this) {
                case ASSIGNED:
                    return newStatus == IN_PROGRESS || newStatus == CANCELLED;
                case IN_PROGRESS:
                    return newStatus == COMPLETED || newStatus == CANCELLED;
                case COMPLETED:
                    return false; // Cannot change from completed
                case CANCELLED:
                    return false; // Cannot change from cancelled
                default:
                    return false;
            }
        }
    }

    public enum TaskPriority {
        LOW("low", "Low priority", 1),
        MEDIUM("medium", "Medium priority", 2),
        HIGH("high", "High priority", 3),
        CRITICAL("critical", "Critical priority", 4);

        private final String value;
        private final String displayName;
        private final int level;

        TaskPriority(String value, String displayName, int level) {
            this.value = value;
            this.displayName = displayName;
            this.level = level;
        }

        public String getValue() { return value; }
        public String getDisplayName() { return displayName; }
        public int getLevel() { return level; }

        public boolean isHigherThan(TaskPriority other) {
            return this.level > other.level;
        }

        public boolean isLowerThan(TaskPriority other) {
            return this.level < other.level;
        }
    }

    // Constructors
    public TaskAssignment() {}

    public TaskAssignment(String taskTitle, String taskDescription, UUID assignedBy, UUID assignedTo, UUID projectId) {
        this.taskTitle = taskTitle;
        this.taskDescription = taskDescription;
        this.assignedBy = assignedBy;
        this.assignedTo = assignedTo;
        this.projectId = projectId;
    }

    // Business methods
    public boolean isOverdue() {
        return dueDate != null && 
               LocalDateTime.now().isAfter(dueDate) && 
               status != TaskStatus.COMPLETED && 
               status != TaskStatus.CANCELLED;
    }

    public boolean isDueSoon(int daysThreshold) {
        return dueDate != null && 
               LocalDateTime.now().plusDays(daysThreshold).isAfter(dueDate) &&
               status != TaskStatus.COMPLETED && 
               status != TaskStatus.CANCELLED;
    }

    public boolean canEdit() {
        return status == TaskStatus.ASSIGNED || status == TaskStatus.IN_PROGRESS;
    }

    public boolean canComplete() {
        return status == TaskStatus.IN_PROGRESS;
    }

    public boolean canCancel() {
        return status == TaskStatus.ASSIGNED || status == TaskStatus.IN_PROGRESS;
    }

    public void assign(UUID assignedBy, UUID assignedTo) {
        this.assignedBy = assignedBy;
        this.assignedTo = assignedTo;
        this.status = TaskStatus.ASSIGNED;
    }

    public void startWork() {
        if (canEdit()) {
            this.status = TaskStatus.IN_PROGRESS;
        }
    }

    public void complete(String completionNotes) {
        if (canComplete()) {
            this.status = TaskStatus.COMPLETED;
            this.completedAt = LocalDateTime.now();
            this.completionNotes = completionNotes;
        }
    }

    public void cancel(String reason) {
        if (canCancel()) {
            this.status = TaskStatus.CANCELLED;
            this.notes = (this.notes != null ? this.notes + "\n" : "") + 
                        "Cancelled: " + reason;
        }
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
        if (dueDate != null && priority == TaskPriority.LOW && isOverdue()) {
            this.priority = TaskPriority.HIGH;
        }
    }

    public void updateEstimatedHours(Integer hours) {
        this.estimatedHours = hours;
    }

    public void updateActualHours(Integer hours) {
        this.actualHours = hours;
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

    public void addBlocker(String blocker) {
        if (this.blockers == null) {
            this.blockers = new java.util.ArrayList<>();
        }
        if (!this.blockers.contains(blocker)) {
            this.blockers.add(blocker);
        }
    }

    public void resolveBlocker(String blocker) {
        if (this.blockers != null) {
            this.blockers.remove(blocker);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaskAssignment that = (TaskAssignment) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "TaskAssignment{" +
                "id=" + id +
                ", taskTitle='" + taskTitle + '\'' +
                ", assignedTo=" + assignedTo +
                ", status=" + status +
                ", priority=" + priority +
                ", dueDate=" + dueDate +
                ", createdAt=" + createdAt +
                '}';
    }
}