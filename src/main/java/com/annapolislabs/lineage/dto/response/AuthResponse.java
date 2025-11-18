package com.annapolislabs.lineage.dto.response;

import com.annapolislabs.lineage.entity.UserRole;
import com.annapolislabs.lineage.entity.UserStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Setter
@Getter
public class AuthResponse {

    // Getters and Setters
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

}
