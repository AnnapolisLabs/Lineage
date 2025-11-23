package com.annapolislabs.lineage.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class MfaSetupResponse {

    // Getters and Setters
    private String secretKey;
    private String qrCodeUrl;
    private List<String> backupCodes;
    private boolean setupComplete;
    
    // Constructors
    public MfaSetupResponse() {}
    
    public MfaSetupResponse(String secretKey, String qrCodeUrl, List<String> backupCodes) {
        this.secretKey = secretKey;
        this.qrCodeUrl = qrCodeUrl;
        this.backupCodes = backupCodes;
    }

}