package com.annapolislabs.lineage.service;

import com.annapolislabs.lineage.entity.*;
import com.annapolislabs.lineage.repository.*;
import com.annapolislabs.lineage.security.SecurityAuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for peer review management operations
 * Handles review creation, assignment, approval workflows, and review tracking
 */
@Slf4j
@Service
public class PeerReviewService {

    @Autowired
    private PeerReviewRepository peerReviewRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PermissionEvaluationService permissionEvaluationService;

    @Autowired
    private SecurityAuditService securityAuditService;

    @Autowired
    private EmailService emailService;

    /**
     * Create a new peer review
     */
    @Transactional
    public PeerReview createReview(UUID requirementId, UUID reviewerId, UUID authorId, 
                                  PeerReview.ReviewType reviewType, LocalDateTime deadline, UUID requestingUserId) {
        log.info("Creating peer review for requirement {} with reviewer {} by user {}", 
                requirementId, reviewerId, requestingUserId);

        // Validate inputs
        if (requirementId == null) {
            throw new IllegalArgumentException("Requirement ID is required");
        }
        if (reviewerId == null) {
            throw new IllegalArgumentException("Reviewer ID is required");
        }
        if (authorId == null) {
            throw new IllegalArgumentException("Author ID is required");
        }

        // Check if requesting user has permission to create reviews
        if (!permissionEvaluationService.hasPermission(requestingUserId, "peer.review", null)) {
            throw new SecurityException("User does not have permission to create peer reviews");
        }

        // Validate users exist and are active
        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new IllegalArgumentException("Reviewer user not found: " + reviewerId));
        
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("Author user not found: " + authorId));

        if (reviewer.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Cannot assign reviews to inactive user");
        }
        if (author.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Cannot create reviews for inactive author");
        }

        // Check if review already exists
        if (peerReviewRepository.existsByRequirementIdAndReviewerId(requirementId, reviewerId)) {
            throw new IllegalArgumentException("Review already exists for this requirement and reviewer");
        }

        // Create review
        PeerReview review = new PeerReview(requirementId, reviewerId, authorId, reviewType);
        review.setReviewDeadline(deadline);

        // Save review
        review = peerReviewRepository.save(review);

        // Send review assignment email
        try {
            emailService.sendPeerReviewInvitation(
                reviewer.getEmail(),
                author.getEmail(),
                requirementId.toString(),
                reviewType.getDisplayName(),
                review.getId().toString()
            );
        } catch (Exception e) {
            log.error("Failed to send peer review invitation email to {}", reviewer.getEmail(), e);
            // Don't fail the operation if email fails
        }

        // Audit log
        securityAuditService.logEvent("PEER_REVIEW_CREATED", requestingUserId, "PEER_REVIEW", review.getId(),
                Map.of("requirement_id", requirementId, "reviewer_id", reviewerId, "author_id", authorId, 
                       "review_type", reviewType.name(), "deadline", deadline));

        log.info("Peer review created successfully with ID {}", review.getId());
        return review;
    }

    /**
     * Get peer review by ID
     */
    public PeerReview getPeerReviewById(UUID reviewId, UUID requestingUserId) {
        log.debug("Getting peer review {} for user {}", reviewId, requestingUserId);

        PeerReview review = peerReviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Peer review not found: " + reviewId));

        // Check if user has permission to view this review
        boolean canView = review.getReviewerId().equals(requestingUserId) ||
                         review.getAuthorId().equals(requestingUserId) ||
                         permissionEvaluationService.hasPermission(requestingUserId, "peer.review", null);

        if (!canView) {
            throw new SecurityException("User does not have permission to view this peer review");
        }

        return review;
    }

    /**
     * Get reviews by requirement ID
     */
    public List<PeerReview> getReviewsByRequirement(UUID requirementId, UUID requestingUserId) {
        log.debug("Getting reviews for requirement {} by user {}", requirementId, requestingUserId);

        // Check if user has permission to view reviews for this requirement
        if (!permissionEvaluationService.hasPermission(requestingUserId, "peer.read", null)) {
            throw new SecurityException("User does not have permission to view peer reviews");
        }

        return peerReviewRepository.findByRequirementIdAndStatusIn(requirementId, 
                Arrays.asList(PeerReview.ReviewStatus.values()));
    }

    /**
     * Start a peer review
     */
    @Transactional
    public void startReview(UUID reviewId, UUID requestingUserId) {
        log.info("Starting peer review {} by user {}", reviewId, requestingUserId);

        PeerReview review = peerReviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Peer review not found: " + reviewId));

        // Only reviewer can start the review
        if (!review.getReviewerId().equals(requestingUserId)) {
            throw new SecurityException("Only the assigned reviewer can start this peer review");
        }

        if (!review.canEdit()) {
            throw new IllegalArgumentException("Review cannot be started in current status: " + review.getStatus());
        }

        review.startReview();
        peerReviewRepository.save(review);

        // Audit log
        securityAuditService.logEvent("PEER_REVIEW_STARTED", requestingUserId, "PEER_REVIEW", reviewId,
                Map.of("previous_status", "pending", "new_status", "in_progress"));

        log.info("Peer review {} started successfully", reviewId);
    }

    /**
     * Approve a peer review
     */
    @Transactional
    public void approveReview(UUID reviewId, String comments, UUID requestingUserId) {
        log.info("Approving peer review {} by user {}", reviewId, requestingUserId);

        PeerReview review = peerReviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Peer review not found: " + reviewId));

        // Only reviewer can approve the review
        if (!review.getReviewerId().equals(requestingUserId)) {
            throw new SecurityException("Only the assigned reviewer can approve this peer review");
        }

        if (!review.canComplete()) {
            throw new IllegalArgumentException("Review cannot be approved in current status: " + review.getStatus());
        }

        review.approve(comments);
        peerReviewRepository.save(review);

        // Send approval notification email
        try {
            User reviewer = userRepository.findById(review.getReviewerId()).orElse(null);
            User author = userRepository.findById(review.getAuthorId()).orElse(null);
            
            if (reviewer != null && author != null) {
                emailService.sendPeerReviewApprovalNotification(
                    author.getEmail(),
                    reviewer.getEmail(),
                    reviewId.toString(),
                    comments
                );
            }
        } catch (Exception e) {
            log.error("Failed to send peer review approval notification", e);
        }

        // Audit log
        securityAuditService.logEvent("PEER_REVIEW_APPROVED", requestingUserId, "PEER_REVIEW", reviewId,
                Map.of("comments", comments));

        log.info("Peer review {} approved successfully", reviewId);
    }

    /**
     * Reject a peer review
     */
    @Transactional
    public void rejectReview(UUID reviewId, String comments, UUID requestingUserId) {
        log.info("Rejecting peer review {} by user {}", reviewId, requestingUserId);

        PeerReview review = peerReviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Peer review not found: " + reviewId));

        // Only reviewer can reject the review
        if (!review.getReviewerId().equals(requestingUserId)) {
            throw new SecurityException("Only the assigned reviewer can reject this peer review");
        }

        if (!review.canComplete()) {
            throw new IllegalArgumentException("Review cannot be rejected in current status: " + review.getStatus());
        }

        review.reject(comments);
        peerReviewRepository.save(review);

        // Send rejection notification email
        try {
            User reviewer = userRepository.findById(review.getReviewerId()).orElse(null);
            User author = userRepository.findById(review.getAuthorId()).orElse(null);
            
            if (reviewer != null && author != null) {
                emailService.sendPeerReviewRejectionNotification(
                    author.getEmail(),
                    reviewer.getEmail(),
                    reviewId.toString(),
                    comments
                );
            }
        } catch (Exception e) {
            log.error("Failed to send peer review rejection notification", e);
        }

        // Audit log
        securityAuditService.logEvent("PEER_REVIEW_REJECTED", requestingUserId, "PEER_REVIEW", reviewId,
                Map.of("comments", comments));

        log.info("Peer review {} rejected successfully", reviewId);
    }

    /**
     * Request revision for a peer review
     */
    @Transactional
    public void requestRevision(UUID reviewId, String comments, UUID requestingUserId) {
        log.info("Requesting revision for peer review {} by user {}", reviewId, requestingUserId);

        PeerReview review = peerReviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Peer review not found: " + reviewId));

        // Only reviewer can request revision
        if (!review.getReviewerId().equals(requestingUserId)) {
            throw new SecurityException("Only the assigned reviewer can request revision for this peer review");
        }

        if (!review.canComplete()) {
            throw new IllegalArgumentException("Review cannot request revision in current status: " + review.getStatus());
        }

        review.requestRevision(comments);
        peerReviewRepository.save(review);

        // Send revision notification email
        try {
            User reviewer = userRepository.findById(review.getReviewerId()).orElse(null);
            User author = userRepository.findById(review.getAuthorId()).orElse(null);
            
            if (reviewer != null && author != null) {
                emailService.sendPeerReviewRevisionRequestNotification(
                    author.getEmail(),
                    reviewer.getEmail(),
                    reviewId.toString(),
                    comments
                );
            }
        } catch (Exception e) {
            log.error("Failed to send peer review revision notification", e);
        }

        // Audit log
        securityAuditService.logEvent("PEER_REVIEW_REVISION_REQUESTED", requestingUserId, "PEER_REVIEW", reviewId,
                Map.of("comments", comments));

        log.info("Peer review {} revision requested successfully", reviewId);
    }

    /**
     * Set ratings for a peer review
     */
    @Transactional
    public void setRatings(UUID reviewId, Integer effortRating, Integer qualityRating, UUID requestingUserId) {
        log.info("Setting ratings for peer review {} by user {}", reviewId, requestingUserId);

        PeerReview review = peerReviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Peer review not found: " + reviewId));

        // Only reviewer can set ratings
        if (!review.getReviewerId().equals(requestingUserId)) {
            throw new SecurityException("Only the assigned reviewer can set ratings for this peer review");
        }

        if (!review.canEdit()) {
            throw new IllegalArgumentException("Ratings cannot be set in current review status: " + review.getStatus());
        }

        review.setRatings(effortRating, qualityRating);
        peerReviewRepository.save(review);

        // Audit log
        securityAuditService.logEvent("PEER_REVIEW_RATINGS_SET", requestingUserId, "PEER_REVIEW", reviewId,
                Map.of("effort_rating", effortRating, "quality_rating", qualityRating));

        log.info("Ratings set successfully for peer review {}: effort={}, quality={}", reviewId, effortRating, qualityRating);
    }

    /**
     * Get pending reviews for a reviewer
     */
    public List<PeerReview> getPendingReviews(UUID reviewerId, UUID requestingUserId) {
        log.debug("Getting pending reviews for reviewer {} by user {}", reviewerId, requestingUserId);

        // Check if user can view their own reviews or has admin permission
        if (!reviewerId.equals(requestingUserId) && 
            !permissionEvaluationService.hasPermission(requestingUserId, "peer.review", null)) {
            throw new SecurityException("User does not have permission to view these reviews");
        }

        return peerReviewRepository.findByReviewerIdAndStatusIn(reviewerId, 
                Arrays.asList(PeerReview.ReviewStatus.PENDING));
    }

    /**
     * Get overdue reviews
     */
    public List<PeerReview> getOverdueReviews(UUID requestingUserId) {
        log.debug("Getting overdue reviews for user {}", requestingUserId);

        // Check if user has permission to view reviews
        if (!permissionEvaluationService.hasPermission(requestingUserId, "peer.read", null)) {
            throw new SecurityException("User does not have permission to view peer reviews");
        }

        return peerReviewRepository.findOverdueReviews(LocalDateTime.now());
    }

    /**
     * Get reviews needing attention (pending or overdue)
     */
    public List<PeerReview> getReviewsNeedingAttention(UUID requestingUserId) {
        log.debug("Getting reviews needing attention for user {}", requestingUserId);

        // Check if user has permission to view reviews
        if (!permissionEvaluationService.hasPermission(requestingUserId, "peer.read", null)) {
            throw new SecurityException("User does not have permission to view peer reviews");
        }

        return peerReviewRepository.findReviewsNeedingAttention(LocalDateTime.now());
    }

    /**
     * Search peer reviews
     */
    public Page<PeerReview> searchReviews(String searchTerm, UUID reviewerId, UUID authorId, 
                                        PeerReview.ReviewStatus status, PeerReview.ReviewType reviewType,
                                        Pageable pageable, UUID requestingUserId) {
        log.debug("Searching reviews with term '{}' by user {}", searchTerm, requestingUserId);

        // Check if user has permission to search reviews
        if (!permissionEvaluationService.hasPermission(requestingUserId, "peer.read", null)) {
            throw new SecurityException("User does not have permission to search peer reviews");
        }

        if (StringUtils.hasText(searchTerm)) {
            return peerReviewRepository.findBySearch(searchTerm, pageable);
        } else {
            return peerReviewRepository.findWithFilters(null, reviewerId, authorId, status, reviewType, pageable);
        }
    }

    /**
     * Get peer review statistics
     */
    public Map<String, Object> getPeerReviewStatistics(UUID requestingUserId) {
        log.debug("Getting peer review statistics for user {}", requestingUserId);

        // Check if user has permission to view review statistics
        if (!permissionEvaluationService.hasPermission(requestingUserId, "peer.read", null)) {
            throw new SecurityException("User does not have permission to view peer review statistics");
        }

        Map<String, Object> statistics = new HashMap<>();

        // Get counts by status
        List<Object[]> statusCounts = peerReviewRepository.getReviewCountByStatus();
        Map<String, Long> statusMap = statusCounts.stream()
                .collect(Collectors.toMap(obj -> ((PeerReview.ReviewStatus) obj[0]).name(), obj -> (Long) obj[1]));
        statistics.put("by_status", statusMap);

        // Get counts by type
        List<Object[]> typeCounts = peerReviewRepository.getReviewCountByType();
        Map<String, Long> typeMap = typeCounts.stream()
                .collect(Collectors.toMap(obj -> ((PeerReview.ReviewType) obj[0]).name(), obj -> (Long) obj[1]));
        statistics.put("by_type", typeMap);

        // Get pending reviews per reviewer
        List<Object[]> pendingCounts = peerReviewRepository.getPendingReviewCountByReviewer();
        Map<String, Long> pendingMap = pendingCounts.stream()
                .collect(Collectors.toMap(obj -> obj[0].toString(), obj -> (Long) obj[1]));
        statistics.put("pending_by_reviewer", pendingMap);

        // Get overdue reviews count
        List<PeerReview> overdueReviews = peerReviewRepository.findOverdueReviews(LocalDateTime.now());
        statistics.put("overdue_count", (long) overdueReviews.size());

        return statistics;
    }
}