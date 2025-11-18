package com.annapolislabs.lineage.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class VerifyEmailRequest {

    // Getters and Setters
    @NotBlank(message = "Verification token is required")
    private String token;
    
    // Constructors
    public VerifyEmailRequest() {}
    
    public VerifyEmailRequest(String token) {
        this.token = token;
    }

}