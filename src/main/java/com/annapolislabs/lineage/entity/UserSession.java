package com.annapolislabs.lineage.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Getter
@Entity
@Table(name = "user_sessions", indexes = {
    @Index(name = "idx_user_sessions_user_id", columnList = "user_id"),
    @Index(name = "idx_user_sessions_session_id", columnList = "session_id", unique = true),
    @Index(name = "idx_user_sessions_token_hash", columnList = "token_hash"),
    @Index(name = "idx_user_sessions_expires_at", columnList = "expires_at"),
    @Index(name = "idx_user_sessions_revoked", columnList = "revoked")
})
@EntityListeners(AuditingEntityListener.class)
public class UserSession {

    // Getters and Setters
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
    private User user;

    @NotBlank
    @Column(name = "session_id", nullable = false, unique = true, length = 255)
    private String sessionId;

    @NotBlank
    @Column(name = "token_hash", nullable = false, length = 255)
    @JsonIgnore
    private String tokenHash;

    @Column(name = "refresh_token_hash", length = 255)
    @JsonIgnore
    private String refreshTokenHash;

    @NotNull
    @Column(name = "expires_at", nullable = false, columnDefinition = "timestamp")
    private LocalDateTime expiresAt;

    @Column(name = "refresh_expires_at", columnDefinition = "timestamp")
    private LocalDateTime refreshExpiresAt;

    @Column(name = "last_activity_at", columnDefinition = "timestamp")
    private LocalDateTime lastActivityAt;

    @Size(max = 45)
    @Column(name = "ip_address", length = 45)
    private String ipAddress; // IPv6 compatible

    @Column(name = "user_agent", columnDefinition = "text")
    private String userAgent;

    @Column(name = "device_info", columnDefinition = "jsonb")
    private String deviceInfo; // JSON string for device details

    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;

    @Column(name = "revoked_at", columnDefinition = "timestamp")
    private LocalDateTime revokedAt;

    @Size(max = 255)
    @Column(name = "revoked_reason", length = 255)
    private String revokedReason;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Constructors
    public UserSession() {}

    public UserSession(UUID userId, String sessionId, String tokenHash, LocalDateTime expiresAt, String ipAddress, String userAgent) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.lastActivityAt = LocalDateTime.now();
    }

    // Utility Methods
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isRefreshExpired() {
        return refreshExpiresAt != null && LocalDateTime.now().isAfter(refreshExpiresAt);
    }

    public boolean isActive() {
        return !revoked && !isExpired();
    }

    public boolean canRefresh() {
        return !isExpired() && !isRefreshExpired() && refreshTokenHash != null;
    }

    public void updateActivity() {
        this.lastActivityAt = LocalDateTime.now();
    }

    public void revoke(String reason) {
        this.revoked = true;
        this.revokedAt = LocalDateTime.now();
        this.revokedReason = reason;
    }

    public void extendSession(LocalDateTime newExpiresAt) {
        if (refreshExpiresAt != null) {
            // Calculate extension duration before updating expiresAt
            long extensionDuration = java.time.Duration.between(expiresAt, newExpiresAt).getSeconds();
            // Extend refresh token by same amount
            this.refreshExpiresAt = refreshExpiresAt.plusSeconds(extensionDuration);
        }
        this.expiresAt = newExpiresAt;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public void setRefreshTokenHash(String refreshTokenHash) {
        this.refreshTokenHash = refreshTokenHash;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public void setRefreshExpiresAt(LocalDateTime refreshExpiresAt) {
        this.refreshExpiresAt = refreshExpiresAt;
    }

    public void setLastActivityAt(LocalDateTime lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    public void setRevokedAt(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }

    public void setRevokedReason(String revokedReason) {
        this.revokedReason = revokedReason;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserSession that = (UserSession) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "UserSession{" +
                "id=" + id +
                ", userId=" + userId +
                ", sessionId='" + sessionId + '\'' +
                ", expiresAt=" + expiresAt +
                ", refreshExpiresAt=" + refreshExpiresAt +
                ", lastActivityAt=" + lastActivityAt +
                ", ipAddress='" + ipAddress + '\'' +
                ", revoked=" + revoked +
                ", revokedAt=" + revokedAt +
                ", createdAt=" + createdAt +
                '}';
    }
}