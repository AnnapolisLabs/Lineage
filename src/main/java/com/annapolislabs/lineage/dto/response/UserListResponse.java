package com.annapolislabs.lineage.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Lightweight wrapper used by admin user-management views to present
 * a paginated list of users. This mirrors the shape expected by the
 * frontend {@code UserListResponse} type defined in adminService.ts
 * (users, page, size, total, totalPages).
 */
@Getter
@Setter
public class UserListResponse {

    /**
     * Current page of users.
     */
    private List<UserProfileResponse> users;

    /**
     * Zero-based page index.
     */
    private int page;

    /**
     * Requested page size.
     */
    private int size;

    /**
     * Total number of users across all pages.
     */
    private long total;

    /**
     * Total number of pages available.
     */
    private int totalPages;
}
