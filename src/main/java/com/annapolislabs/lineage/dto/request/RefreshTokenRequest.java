package com.annapolislabs.lineage.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RefreshTokenRequest {

    // Getters and Setters
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
    
    // Constructors
    public RefreshTokenRequest() {}
    
    public RefreshTokenRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }

}