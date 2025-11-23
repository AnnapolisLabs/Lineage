package com.annapolislabs.lineage.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ChangePasswordRequest {

    // Getters and Setters
    @NotBlank(message = "Current password is required")
    private String currentPassword;
    
    @NotBlank(message = "New password is required")
    @Size(min = 12, max = 128, message = "New password must be between 12 and 128 characters")
    private String newPassword;
    
    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;
    // Constructors
    /**
     * Default constructor required for JSON deserialization.
     * Field values are set via setters after construction.
     */
    public ChangePasswordRequest() {}


}