package com.annapolislabs.lineage.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.hypersistence.utils.hibernate.type.json.JsonType;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "user_security", indexes = {
    @Index(name = "idx_user_security_user_id", columnList = "user_id"),
    @Index(name = "idx_user_security_mfa_enabled", columnList = "mfa_enabled")
})
@EntityListeners(AuditingEntityListener.class)
public class UserSecurity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
    private User user;

    // MFA Settings
    @Size(max = 255)
    @Column(name = "secret_key", length = 255)
    @JsonIgnore
    private String secretKey;

    @Column(name = "mfa_enabled", nullable = false)
    private boolean mfaEnabled = false;

    @Column(name = "mfa_backup_codes", columnDefinition = "text")
    @JsonIgnore
    private String mfaBackupCodes; // Encrypted backup codes

    @Column(name = "mfa_enabled_at", columnDefinition = "timestamp")
    private LocalDateTime mfaEnabledAt;

    // Security Preferences (JSONB)
    @Type(JsonType.class)
    @Column(name = "security_preferences", columnDefinition = "jsonb")
    private Map<String, Object> securityPreferences = Map.of(
        "sessionTimeout", 30, // minutes
        "requirePasswordForSensitiveActions", true,
        "loginNotifications", true,
        "deviceTrustDuration", 30 // days
    );

    // Device Tracking
    @Size(max = 255)
    @Column(name = "device_fingerprint", length = 255)
    private String deviceFingerprint;

    @Column(name = "last_security_check", columnDefinition = "timestamp")
    private LocalDateTime lastSecurityCheck;

    // Audit Fields
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public UserSecurity() {}

    public UserSecurity(UUID userId) {
        this.userId = userId;
    }

    public UserSecurity(UUID userId, String secretKey, boolean mfaEnabled) {
        this.userId = userId;
        this.secretKey = secretKey;
        this.mfaEnabled = mfaEnabled;
        this.mfaEnabledAt = mfaEnabled ? LocalDateTime.now() : null;
        this.lastSecurityCheck = LocalDateTime.now();
    }

    // Utility Methods
    public boolean isMfaSetupComplete() {
        return mfaEnabled && secretKey != null && !secretKey.isBlank();
    }

    public boolean hasValidBackupCodes() {
        return mfaBackupCodes != null && !mfaBackupCodes.isBlank();
    }

    public void enableMfa(String secretKey, String backupCodes) {
        this.secretKey = secretKey;
        this.mfaBackupCodes = backupCodes;
        this.mfaEnabled = true;
        this.mfaEnabledAt = LocalDateTime.now();
        this.lastSecurityCheck = LocalDateTime.now();
    }

    public void disableMfa() {
        this.mfaEnabled = false;
        this.secretKey = null;
        this.mfaBackupCodes = null;
        this.mfaEnabledAt = null;
    }

    public void updateSecurityCheck() {
        this.lastSecurityCheck = LocalDateTime.now();
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public boolean isMfaEnabled() {
        return mfaEnabled;
    }

    public void setMfaEnabled(boolean mfaEnabled) {
        this.mfaEnabled = mfaEnabled;
        if (mfaEnabled && this.mfaEnabledAt == null) {
            this.mfaEnabledAt = LocalDateTime.now();
        }
        if (!mfaEnabled) {
            this.mfaEnabledAt = null;
        }
    }

    public String getMfaBackupCodes() {
        return mfaBackupCodes;
    }

    public void setMfaBackupCodes(String mfaBackupCodes) {
        this.mfaBackupCodes = mfaBackupCodes;
    }

    public LocalDateTime getMfaEnabledAt() {
        return mfaEnabledAt;
    }

    public void setMfaEnabledAt(LocalDateTime mfaEnabledAt) {
        this.mfaEnabledAt = mfaEnabledAt;
    }

    public Map<String, Object> getSecurityPreferences() {
        return securityPreferences;
    }

    public void setSecurityPreferences(Map<String, Object> securityPreferences) {
        this.securityPreferences = securityPreferences;
    }

    public String getDeviceFingerprint() {
        return deviceFingerprint;
    }

    public void setDeviceFingerprint(String deviceFingerprint) {
        this.deviceFingerprint = deviceFingerprint;
    }

    public LocalDateTime getLastSecurityCheck() {
        return lastSecurityCheck;
    }

    public void setLastSecurityCheck(LocalDateTime lastSecurityCheck) {
        this.lastSecurityCheck = lastSecurityCheck;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserSecurity that = (UserSecurity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "UserSecurity{" +
                "id=" + id +
                ", userId=" + userId +
                ", mfaEnabled=" + mfaEnabled +
                ", mfaEnabledAt=" + mfaEnabledAt +
                ", lastSecurityCheck=" + lastSecurityCheck +
                ", createdAt=" + createdAt +
                '}';
    }
}