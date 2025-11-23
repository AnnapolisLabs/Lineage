package com.annapolislabs.lineage.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for resending email verification
 */
@Setter
@Getter
public class ResendVerificationRequest {

    // Getters and Setters
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    // Constructors
    public ResendVerificationRequest() {}

    public ResendVerificationRequest(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "ResendVerificationRequest{" +
                "email='" + email + '\'' +
                '}';
    }
}