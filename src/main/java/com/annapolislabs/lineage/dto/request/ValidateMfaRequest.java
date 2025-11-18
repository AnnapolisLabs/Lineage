package com.annapolislabs.lineage.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ValidateMfaRequest {

    // Getters and Setters
    @NotBlank(message = "MFA code is required")
    @Size(min = 6, max = 6, message = "MFA code must be 6 digits")
    private String mfaCode;
    
    private String backupCode;

}