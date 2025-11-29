package com.annapolislabs.lineage.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Admin-side request to directly set or reset a user's password.
 *
 * <p>This bypasses the normal "old password" check and is restricted to
 * administrative callers. The password is still validated using the
 * standard password policy.</p>
 */
public class AdminSetPasswordRequest {

    @NotBlank(message = "New password is required")
    @Size(min = 12, max = 128, message = "Password must be between 12 and 128 characters")
    private String newPassword;

    public AdminSetPasswordRequest() {
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
