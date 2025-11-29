package com.annapolislabs.lineage.controller;

import com.annapolislabs.lineage.dto.request.AdminCreateUserRequest;
import com.annapolislabs.lineage.dto.request.AdminSetPasswordRequest;
import com.annapolislabs.lineage.dto.request.UpdateUserRequest;
import com.annapolislabs.lineage.dto.response.UserListResponse;
import com.annapolislabs.lineage.dto.response.UserProfileResponse;
import com.annapolislabs.lineage.entity.UserRole;
import com.annapolislabs.lineage.entity.UserStatus;
import com.annapolislabs.lineage.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Administrative endpoints for managing users.
 *
 * <p>Security for this controller is enforced via
 * {@link com.annapolislabs.lineage.config.SecurityConfig}, which restricts
 * access to callers with the ADMIN role for {@code /api/admin/**} paths.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Users", description = "Administrative user management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {

    private final UserService userService;

    /**
     * Returns a paginated list of users for admin management screens.
     *
     * <p>The response structure matches the frontend's
     * {@code UserListResponse} contract used in {@code adminService.ts}:
     *
     * <pre>
     * {
     *   "users": [ ... ],
     *   "page": 0,
     *   "size": 20,
     *   "total": 42,
     *   "totalPages": 3
     * }
     * </pre>
     */
    @GetMapping("/users")
    @Operation(
        summary = "List users for admin",
        description = "Retrieve a paginated list of users with optional filtering and search for administration."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
    })
    public ResponseEntity<UserListResponse> getUsers(
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Optional free-text search term")
            @RequestParam(required = false) String search,

            @Parameter(description = "Optional filter by user status")
            @RequestParam(required = false) UserStatus status,

            @Parameter(description = "Optional filter by global role")
            @RequestParam(required = false) UserRole role,

            @Parameter(description = "Optional filter by email verification flag")
            @RequestParam(required = false) Boolean verified
    ) {
        Pageable pageable = PageRequest.of(page, size);

        Page<UserProfileResponse> resultPage;
        if (StringUtils.hasText(search) || status != null || role != null || verified != null) {
            resultPage = userService.searchUsers(search, status, role, verified, pageable);
        } else {
            resultPage = userService.getAllUsers(pageable);
        }

        UserListResponse response = new UserListResponse();
        response.setUsers(resultPage.getContent());
        response.setPage(resultPage.getNumber());
        response.setSize(resultPage.getSize());
        response.setTotal(resultPage.getTotalElements());
        response.setTotalPages(resultPage.getTotalPages());

        return ResponseEntity.ok(response);
    }

    /**
     * Returns details for a single user for admin view.
     */
    @GetMapping("/users/{userId}")
    @Operation(summary = "Get user details for admin")
    public ResponseEntity<UserProfileResponse> getUserById(
            @Parameter(description = "User ID", required = true)
            @PathVariable UUID userId) {

        UserProfileResponse profile = userService.getUserProfile(userId);
        return ResponseEntity.ok(profile);
    }

    /**
     * Updates admin-editable fields of a user.
     */
    @PutMapping("/users/{userId}")
    @Operation(summary = "Update user (admin)")
    public ResponseEntity<UserProfileResponse> updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRequest request) {

        UUID adminId = getCurrentUserId();
        UserProfileResponse updated = userService.updateUser(userId, request, adminId);
        return ResponseEntity.ok(updated);
    }

    /**
     * Soft-deletes (deactivates) a user account.
     */
    @DeleteMapping("/users/{userId}")
    @Operation(summary = "Soft delete (deactivate) user")
    public ResponseEntity<Void> deactivateUser(@PathVariable UUID userId) {
        UUID adminId = getCurrentUserId();
        userService.deactivateUser(userId, adminId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Hard delete / purge a user for legal compliance. This operation anonymizes
     * personally identifiable information while preserving referential integrity
     * where possible.
     */
    @DeleteMapping("/users/{userId}/purge")
    @Operation(summary = "Hard delete (purge) user data for compliance")
    public ResponseEntity<Void> purgeUser(@PathVariable UUID userId) {
        UUID adminId = getCurrentUserId();
        userService.purgeUser(userId, adminId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * Explicitly locks a user account until manual unlock.
     */
    @PostMapping("/users/{userId}/lock")
    @Operation(summary = "Lock user account")
    public ResponseEntity<Void> lockUser(@PathVariable UUID userId) {
        UUID adminId = getCurrentUserId();
        userService.lockUser(userId, adminId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Unlocks a previously locked user account and resets failure counters.
     */
    @PostMapping("/users/{userId}/unlock")
    @Operation(summary = "Unlock user account")
    public ResponseEntity<Void> unlockUser(@PathVariable UUID userId) {
        UUID adminId = getCurrentUserId();
        userService.unlockUser(userId, adminId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reactivates a previously deactivated user account.
     *
     * <p>This is distinct from unlocking: deactivation is a soft delete that
     * sets {@link com.annapolislabs.lineage.entity.UserStatus#DEACTIVATED},
     * while locking only affects authentication lockout state. The admin UI
     * uses this endpoint when an operator chooses to "unlock" an account whose
     * status is DEACTIVATED.</p>
     */
    @PostMapping("/users/{userId}/reactivate")
    @Operation(summary = "Reactivate deactivated user account")
    public ResponseEntity<Void> reactivateUser(@PathVariable UUID userId) {
        UUID adminId = getCurrentUserId();
        userService.reactivateUser(userId, adminId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Directly sets or resets a user's password from the admin console.
     *
     * <p>This endpoint is intended for support workflows where an admin needs
     * to establish a new password for a user account. The password is still
     * validated against the standard password policy.</p>
     */
    @PostMapping("/users/{userId}/password")
    @Operation(summary = "Set or reset user password (admin)")
    public ResponseEntity<Void> setUserPassword(@PathVariable UUID userId,
                                                @Valid @RequestBody AdminSetPasswordRequest request) {
        UUID adminId = getCurrentUserId();
        userService.adminSetPassword(userId, request.getNewPassword(), adminId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Creates a new user from the admin console. The request mirrors the
     * frontend CreateUserRequest type and returns a UserProfileResponse so the
     * admin UI can display the created user.
     */
    @PostMapping("/create-user")
    @Operation(summary = "Create user (admin)")
    public ResponseEntity<UserProfileResponse> createUser(
            @Valid @RequestBody AdminCreateUserRequest request) {
        UUID adminId = getCurrentUserId();
        UserProfileResponse created = userService.adminCreateUser(request, adminId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Helper to obtain current authenticated user's ID from security context.
     *
     * <p>JWTs in this application use the email address as the Spring Security
     * username/subject, not the internal UUID. To avoid {@link IllegalArgumentException}
     * from {@code UUID.fromString(...)} when the username is an email, we resolve
     * the {@link java.util.UUID} by looking up the {@link com.annapolislabs.lineage.entity.User}
     * via {@link UserService#getUserByEmail(String)} using {@link Authentication#getName()}.</p>
     */
    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("No authenticated user in context");
        }

        String email = authentication.getName();
        try {
            return userService.getUserByEmail(email).getId();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to resolve current user from authentication context", ex);
        }
    }
}
