package com.annapolislabs.lineage.dto.response;

import com.annapolislabs.lineage.entity.UserRole;
import com.annapolislabs.lineage.entity.UserStatus;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public class AuthResponse {
    
    private boolean success;
    private String message;
    private UUID userId;
    private String email;
    private String token;
    private String refreshToken;
    private UserProfileResponse user;
    private LocalDateTime expiresAt;
    private boolean mfaRequired;
    
    // Constructors
    public AuthResponse() {}
    
    public AuthResponse(String token, String refreshToken, UserProfileResponse user, LocalDateTime expiresAt) {
        this.success = true;
        this.token = token;
        this.refreshToken = refreshToken;
        this.user = user;
        this.expiresAt = expiresAt;
    }
    
    public AuthResponse(boolean success, String message, UUID userId, String email,
                       String token, String refreshToken, UserProfileResponse user) {
        this.success = success;
        this.message = message;
        this.userId = userId;
        this.email = email;
        this.token = token;
        this.refreshToken = refreshToken;
        this.user = user;
    }
    
    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    
    public UserProfileResponse getUser() { return user; }
    public void setUser(UserProfileResponse user) { this.user = user; }
    
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    
    public boolean isMfaRequired() { return mfaRequired; }
    public void setMfaRequired(boolean mfaRequired) { this.mfaRequired = mfaRequired; }
}
