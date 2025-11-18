package com.annapolislabs.lineage.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ValidateMfaRequest {
    
    @NotBlank(message = "MFA code is required")
    @Size(min = 6, max = 6, message = "MFA code must be 6 digits")
    private String mfaCode;
    
    private String backupCode;
    
    // Getters and Setters
    public String getMfaCode() { return mfaCode; }
    public void setMfaCode(String mfaCode) { this.mfaCode = mfaCode; }
    
    public String getBackupCode() { return backupCode; }
    public void setBackupCode(String backupCode) { this.backupCode = backupCode; }
}