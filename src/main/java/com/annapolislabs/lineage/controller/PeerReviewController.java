package com.annapolislabs.lineage.controller;

import com.annapolislabs.lineage.service.PeerReviewService;
import com.annapolislabs.lineage.entity.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * REST controller for peer review management operations
 * Provides endpoints for review creation, assignment, approval workflows, and review tracking
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Peer Review", description = "Peer review creation, approval, and workflow APIs")
public class PeerReviewController {

    private final PeerReviewService peerReviewService;

    /**
     * Get all peer reviews accessible to the current user
     */
    @GetMapping
    @Operation(
        summary = "Get peer reviews", 
        description = "Retrieve peer reviews accessible to the current user with optional filtering"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Peer reviews retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Page<PeerReview>> getPeerReviews(
            @Parameter(description = "Filter by requirement ID")
            @RequestParam(required = false) UUID requirementId,
            
            @Parameter(description = "Filter by reviewer ID")
            @RequestParam(required = false) UUID reviewerId,
            
            @Parameter(description = "Filter by author ID")
            @RequestParam(required = false) UUID authorId,
            
            @Parameter(description = "Filter by review status")
            @RequestParam(required = false) String status,
            
            @Parameter(description = "Filter by review type")
            @RequestParam(required = false) String reviewType,
            
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {
        
        UUID currentUserId = getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        
        PeerReview.ReviewStatus reviewStatus = status != null ? 
                PeerReview.ReviewStatus.valueOf(status.toUpperCase()) : null;
        PeerReview.ReviewType reviewTypeEnum = reviewType != null ? 
                PeerReview.ReviewType.valueOf(reviewType.toUpperCase()) : null;
        
        Page<PeerReview> reviews = peerReviewService.searchReviews(
                null, reviewerId, authorId, reviewStatus, reviewTypeEnum, pageable, currentUserId);
        
        return ResponseEntity.ok(reviews);
    }

    /**
     * Create a new peer review
     */
    @PostMapping
    @Operation(
        summary = "Create peer review", 
        description = "Create a new peer review for a requirement"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Peer review created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
    })
    public ResponseEntity<PeerReview> createPeerReview(
            @Parameter(description = "Peer review creation request", required = true)
            @RequestBody Map<String, Object> request) {
        
        UUID currentUserId = getCurrentUserId();
        
        UUID requirementId = UUID.fromString(request.get("requirement_id").toString());
        UUID reviewerId = UUID.fromString(request.get("reviewer_id").toString());
        UUID authorId = UUID.fromString(request.get("author_id").toString());
        String reviewTypeName = (String) request.get("review_type");
        String deadlineStr = (String) request.get("review_deadline");
        
        PeerReview.ReviewType reviewType = reviewTypeName != null ? 
                PeerReview.ReviewType.valueOf(reviewTypeName.toUpperCase()) : 
                PeerReview.ReviewType.CODE;
        
        LocalDateTime deadline = deadlineStr != null ? 
                LocalDateTime.parse(deadlineStr) : null;
        
        PeerReview review = peerReviewService.createReview(
                requirementId, reviewerId, authorId, reviewType, deadline, currentUserId);
        
        log.info("Peer review created successfully: {}", review.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(review);
    }

    /**
     * Get peer review by ID
     */
    @GetMapping("/{reviewId}")
    @Operation(
        summary = "Get peer review details", 
        description = "Retrieve detailed information about a specific peer review"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Peer review details retrieved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Peer review not found")
    })
    public ResponseEntity<PeerReview> getPeerReview(
            @Parameter(description = "Peer review ID", required = true)
            @PathVariable UUID reviewId) {
        
        UUID currentUserId = getCurrentUserId();
        PeerReview review = peerReviewService.getPeerReviewById(reviewId, currentUserId);
        
        return ResponseEntity.ok(review);
    }

    /**
     * Get reviews for a requirement
     */
    @GetMapping("/requirement/{requirementId}")
    @Operation(
        summary = "Get reviews by requirement", 
        description = "Retrieve all reviews for a specific requirement"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Reviews retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Requirement not found")
    })
    public ResponseEntity<List<PeerReview>> getReviewsByRequirement(
            @Parameter(description = "Requirement ID", required = true)
            @PathVariable UUID requirementId) {
        
        UUID currentUserId = getCurrentUserId();
        List<PeerReview> reviews = peerReviewService.getReviewsByRequirement(requirementId, currentUserId);
        
        return ResponseEntity.ok(reviews);
    }

    /**
     * Start a peer review
     */
    @PostMapping("/{reviewId}/start")
    @Operation(
        summary = "Start peer review", 
        description = "Mark a peer review as in progress"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Peer review started successfully"),
        @ApiResponse(responseCode = "400", description = "Review cannot be started"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Peer review not found")
    })
    public ResponseEntity<Map<String, String>> startReview(
            @Parameter(description = "Peer review ID", required = true)
            @PathVariable UUID reviewId) {
        
        UUID currentUserId = getCurrentUserId();
        peerReviewService.startReview(reviewId, currentUserId);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Peer review started successfully");
        
        log.info("Peer review started successfully: {}", reviewId);
        return ResponseEntity.ok(response);
    }

    /**
     * Approve a peer review
     */
    @PostMapping("/{reviewId}/approve")
    @Operation(
        summary = "Approve peer review", 
        description = "Approve a peer review"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Peer review approved successfully"),
        @ApiResponse(responseCode = "400", description = "Review cannot be approved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Peer review not found")
    })
    public ResponseEntity<Map<String, String>> approveReview(
            @Parameter(description = "Peer review ID", required = true)
            @PathVariable UUID reviewId,
            
            @Parameter(description = "Approval request", required = true)
            @RequestBody Map<String, Object> request) {
        
        UUID currentUserId = getCurrentUserId();
        
        String comments = (String) request.get("comments");
        
        peerReviewService.approveReview(reviewId, comments, currentUserId);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Peer review approved successfully");
        
        log.info("Peer review approved successfully: {}", reviewId);
        return ResponseEntity.ok(response);
    }

    /**
     * Reject a peer review
     */
    @PostMapping("/{reviewId}/reject")
    @Operation(
        summary = "Reject peer review", 
        description = "Reject a peer review"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Peer review rejected successfully"),
        @ApiResponse(responseCode = "400", description = "Review cannot be rejected"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Peer review not found")
    })
    public ResponseEntity<Map<String, String>> rejectReview(
            @Parameter(description = "Peer review ID", required = true)
            @PathVariable UUID reviewId,
            
            @Parameter(description = "Rejection request", required = true)
            @RequestBody Map<String, Object> request) {
        
        UUID currentUserId = getCurrentUserId();
        
        String comments = (String) request.get("comments");
        
        peerReviewService.rejectReview(reviewId, comments, currentUserId);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Peer review rejected successfully");
        
        log.info("Peer review rejected successfully: {}", reviewId);
        return ResponseEntity.ok(response);
    }

    /**
     * Request revision for a peer review
     */
    @PostMapping("/{reviewId}/request-revision")
    @Operation(
        summary = "Request revision", 
        description = "Request revision for a peer review"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Revision requested successfully"),
        @ApiResponse(responseCode = "400", description = "Revision cannot be requested"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Peer review not found")
    })
    public ResponseEntity<Map<String, String>> requestRevision(
            @Parameter(description = "Peer review ID", required = true)
            @PathVariable UUID reviewId,
            
            @Parameter(description = "Revision request", required = true)
            @RequestBody Map<String, Object> request) {
        
        UUID currentUserId = getCurrentUserId();
        
        String comments = (String) request.get("comments");
        
        peerReviewService.requestRevision(reviewId, comments, currentUserId);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Peer review revision requested successfully");
        
        log.info("Peer review revision requested successfully: {}", reviewId);
        return ResponseEntity.ok(response);
    }

    /**
     * Set ratings for a peer review
     */
    @PostMapping("/{reviewId}/ratings")
    @Operation(
        summary = "Set review ratings", 
        description = "Set effort and quality ratings for a peer review"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ratings set successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid ratings"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Peer review not found")
    })
    public ResponseEntity<Map<String, String>> setRatings(
            @Parameter(description = "Peer review ID", required = true)
            @PathVariable UUID reviewId,
            
            @Parameter(description = "Ratings request", required = true)
            @RequestBody Map<String, Object> request) {
        
        UUID currentUserId = getCurrentUserId();
        
        Integer effortRating = (Integer) request.get("effort_rating");
        Integer qualityRating = (Integer) request.get("quality_rating");
        
        peerReviewService.setRatings(reviewId, effortRating, qualityRating, currentUserId);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Ratings set successfully");
        response.put("effort_rating", effortRating.toString());
        response.put("quality_rating", qualityRating.toString());
        
        log.info("Ratings set for peer review {}: effort={}, quality={}", reviewId, effortRating, qualityRating);
        return ResponseEntity.ok(response);
    }

    /**
     * Get pending reviews for current user
     */
    @GetMapping("/pending")
    @Operation(
        summary = "Get pending reviews", 
        description = "Retrieve pending reviews for the current user"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pending reviews retrieved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<PeerReview>> getPendingReviews() {
        UUID currentUserId = getCurrentUserId();
        List<PeerReview> pendingReviews = peerReviewService.getPendingReviews(currentUserId, currentUserId);
        
        return ResponseEntity.ok(pendingReviews);
    }

    /**
     * Get overdue reviews
     */
    @GetMapping("/overdue")
    @Operation(
        summary = "Get overdue reviews", 
        description = "Retrieve reviews that are past their deadline"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Overdue reviews retrieved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<PeerReview>> getOverdueReviews() {
        UUID currentUserId = getCurrentUserId();
        List<PeerReview> overdueReviews = peerReviewService.getOverdueReviews(currentUserId);
        
        return ResponseEntity.ok(overdueReviews);
    }

    /**
     * Get reviews needing attention
     */
    @GetMapping("/needing-attention")
    @Operation(
        summary = "Get reviews needing attention", 
        description = "Retrieve reviews that need attention (pending or overdue)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Reviews needing attention retrieved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<PeerReview>> getReviewsNeedingAttention() {
        UUID currentUserId = getCurrentUserId();
        List<PeerReview> needingAttention = peerReviewService.getReviewsNeedingAttention(currentUserId);
        
        return ResponseEntity.ok(needingAttention);
    }

    /**
     * Search peer reviews
     */
    @GetMapping("/search")
    @Operation(
        summary = "Search peer reviews", 
        description = "Search peer reviews by comments, author, or other criteria"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search results retrieved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Page<PeerReview>> searchReviews(
            @Parameter(description = "Search term")
            @RequestParam(required = false) String search,
            
            @Parameter(description = "Filter by reviewer ID")
            @RequestParam(required = false) UUID reviewerId,
            
            @Parameter(description = "Filter by author ID")
            @RequestParam(required = false) UUID authorId,
            
            @Parameter(description = "Filter by review status")
            @RequestParam(required = false) String status,
            
            @Parameter(description = "Filter by review type")
            @RequestParam(required = false) String reviewType,
            
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {
        
        UUID currentUserId = getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        
        PeerReview.ReviewStatus reviewStatus = status != null ? 
                PeerReview.ReviewStatus.valueOf(status.toUpperCase()) : null;
        PeerReview.ReviewType reviewTypeEnum = reviewType != null ? 
                PeerReview.ReviewType.valueOf(reviewType.toUpperCase()) : null;
        
        Page<PeerReview> reviews = peerReviewService.searchReviews(
                search, reviewerId, authorId, reviewStatus, reviewTypeEnum, pageable, currentUserId);
        
        return ResponseEntity.ok(reviews);
    }

    /**
     * Get peer review statistics
     */
    @GetMapping("/statistics")
    @Operation(
        summary = "Get peer review statistics", 
        description = "Get statistics about peer review usage and performance"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Map<String, Object>> getPeerReviewStatistics() {
        UUID currentUserId = getCurrentUserId();
        Map<String, Object> statistics = peerReviewService.getPeerReviewStatistics(currentUserId);
        
        return ResponseEntity.ok(statistics);
    }

    /**
     * Get current user ID from authentication context
     */
    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("User not authenticated");
        }
        
        Object principal = authentication.getPrincipal();
        if (principal instanceof String) {
            try {
                return UUID.fromString((String) principal);
            } catch (IllegalArgumentException e) {
                throw new SecurityException("Invalid user ID in authentication context");
            }
        }
        
        throw new SecurityException("Unable to extract user ID from authentication context");
    }
}