package com.annapolislabs.lineage.dto.request;

import com.annapolislabs.lineage.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Admin-side user creation request.
 *
 * Mirrors the frontend CreateUserRequest shape used in adminService.ts
 * (email, firstName, lastName, globalRole, sendInvitation).
 */
public class AdminCreateUserRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 254, message = "Email must not exceed 254 characters")
    private String email;

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    @NotNull(message = "Global role is required")
    private UserRole globalRole;

    /**
     * When true the backend should send an invitation / verification email
     * to the newly created user so they can activate their account.
     */
    private boolean sendInvitation;

    public AdminCreateUserRequest() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public UserRole getGlobalRole() {
        return globalRole;
    }

    public void setGlobalRole(UserRole globalRole) {
        this.globalRole = globalRole;
    }

    public boolean isSendInvitation() {
        return sendInvitation;
    }

    public void setSendInvitation(boolean sendInvitation) {
        this.sendInvitation = sendInvitation;
    }
}
