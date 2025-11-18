package com.annapolislabs.lineage.dto.response;

import java.util.List;

public class MfaSetupResponse {
    
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
    
    // Getters and Setters
    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
    
    public String getQrCodeUrl() { return qrCodeUrl; }
    public void setQrCodeUrl(String qrCodeUrl) { this.qrCodeUrl = qrCodeUrl; }
    
    public List<String> getBackupCodes() { return backupCodes; }
    public void setBackupCodes(List<String> backupCodes) { this.backupCodes = backupCodes; }
    
    public boolean isSetupComplete() { return setupComplete; }
    public void setSetupComplete(boolean setupComplete) { this.setupComplete = setupComplete; }
}