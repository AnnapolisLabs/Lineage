package com.annapolislabs.lineage.dto.request;

import jakarta.validation.constraints.NotBlank;

public class VerifyEmailRequest {
    
    @NotBlank(message = "Verification token is required")
    private String token;
    
    // Constructors
    public VerifyEmailRequest() {}
    
    public VerifyEmailRequest(String token) {
        this.token = token;
    }
    
    // Getters and Setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}