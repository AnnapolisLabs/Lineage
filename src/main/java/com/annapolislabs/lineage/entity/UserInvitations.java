package com.annapolislabs.lineage.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "user_invitations", indexes = {
    @Index(name = "idx_user_invitations_token", columnList = "token", unique = true),
    @Index(name = "idx_user_invitations_email", columnList = "email"),
    @Index(name = "idx_user_invitations_project_id", columnList = "project_id"),
    @Index(name = "idx_user_invitations_invited_by", columnList = "invited_by"),
    @Index(name = "idx_user_invitations_status", columnList = "status"),
    @Index(name = "idx_user_invitations_expiry_date", columnList = "expiry_date")
})
@EntityListeners(AuditingEntityListener.class)
public class UserInvitations {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @Column(name = "invited_by", nullable = false)
    private UUID invitedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by", referencedColumnName = "id", insertable = false, updatable = false)
    private User invitedByUser;

    @Email
    @NotBlank
    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "project_id", insertable = false, updatable = false)
    private UUID projectId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", referencedColumnName = "id")
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(name = "project_role", length = 20)
    private ProjectRole projectRole;

    @NotBlank
    @Column(nullable = false, unique = true, length = 255)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvitationStatus status = InvitationStatus.PENDING;

    @NotNull
    @Column(name = "expiry_date", nullable = false, columnDefinition = "timestamp")
    private LocalDateTime expiryDate;

    @Column(name = "accepted_at", columnDefinition = "timestamp")
    private LocalDateTime acceptedAt;

    // Optional message or context
    @Column(length = 500)
    private String message;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Constructors
    public UserInvitations() {}

    public UserInvitations(UUID invitedBy, String email, UUID projectId, ProjectRole projectRole, String token, LocalDateTime expiryDate) {
        this.invitedBy = invitedBy;
        this.email = email;
        this.projectId = projectId;
        this.projectRole = projectRole;
        this.token = token;
        this.expiryDate = expiryDate;
    }

    public UserInvitations(UUID invitedBy, String email, UUID projectId, ProjectRole projectRole, String token, LocalDateTime expiryDate, String message) {
        this(invitedBy, email, projectId, projectRole, token, expiryDate);
        this.message = message;
    }

    // Utility Methods
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }

    public boolean isPending() {
        return status == InvitationStatus.PENDING;
    }

    public boolean isAccepted() {
        return status == InvitationStatus.ACCEPTED;
    }

    public boolean canAccept() {
        return isPending() && !isExpired();
    }

    public void accept() {
        this.status = InvitationStatus.ACCEPTED;
        this.acceptedAt = LocalDateTime.now();
    }

    public void expire() {
        this.status = InvitationStatus.EXPIRED;
    }

    public void cancel() {
        this.status = InvitationStatus.CANCELLED;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getInvitedBy() {
        return invitedBy;
    }

    public void setInvitedBy(UUID invitedBy) {
        this.invitedBy = invitedBy;
    }

    public User getInvitedByUser() {
        return invitedByUser;
    }

    public void setInvitedByUser(User invitedByUser) {
        this.invitedByUser = invitedByUser;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public ProjectRole getProjectRole() {
        return projectRole;
    }

    public void setProjectRole(ProjectRole projectRole) {
        this.projectRole = projectRole;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public InvitationStatus getStatus() {
        return status;
    }

    public void setStatus(InvitationStatus status) {
        this.status = status;
    }

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(LocalDateTime acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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
        UserInvitations that = (UserInvitations) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "UserInvitations{" +
                "id=" + id +
                ", invitedBy=" + invitedBy +
                ", email='" + email + '\'' +
                ", projectId=" + projectId +
                ", projectRole=" + projectRole +
                ", status=" + status +
                ", token='" + token + '\'' +
                ", expiryDate=" + expiryDate +
                ", acceptedAt=" + acceptedAt +
                ", createdAt=" + createdAt +
                '}';
    }
}