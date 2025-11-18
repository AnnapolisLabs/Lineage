package com.annapolislabs.lineage.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Setter
@Getter
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

    // Getters and Setters
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