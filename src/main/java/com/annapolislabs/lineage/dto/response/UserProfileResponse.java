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
public class UserProfileResponse {

    // Getters and Setters
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String name;
    private String phoneNumber;
    private String avatarUrl;
    private String bio;
    private Map<String, Object> preferences;
    private UserStatus status;
    private UserRole globalRole;
    private boolean emailVerified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;
    
    // Constructors
    public UserProfileResponse() {}

}