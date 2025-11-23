package com.annapolislabs.lineage.repository;

import com.annapolislabs.lineage.entity.PeerReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for PeerReview entities
 */
@Repository
public interface PeerReviewRepository extends JpaRepository<PeerReview, UUID> {

    /**
     * Find peer reviews by requirement ID
     */
    List<PeerReview> findByRequirementIdAndStatusIn(UUID requirementId, List<PeerReview.ReviewStatus> statuses);
    
    /**
     * Find peer reviews by reviewer ID
     */
    List<PeerReview> findByReviewerIdAndStatusIn(UUID reviewerId, List<PeerReview.ReviewStatus> statuses);
    
    /**
     * Find peer reviews by author ID
     */
    List<PeerReview> findByAuthorIdAndStatusIn(UUID authorId, List<PeerReview.ReviewStatus> statuses);
    
    /**
     * Find peer review by requirement ID and reviewer ID (for unique constraint)
     */
    Optional<PeerReview> findByRequirementIdAndReviewerId(UUID requirementId, UUID reviewerId);
    
    /**
     * Check if peer review exists by requirement ID and reviewer ID
     */
    boolean existsByRequirementIdAndReviewerId(UUID requirementId, UUID reviewerId);
    
    /**
     * Find pending reviews
     */
    @Query("SELECT pr FROM PeerReview pr WHERE pr.status = 'pending'")
    Page<PeerReview> findPendingReviews(Pageable pageable);
    
    /**
     * Find overdue reviews
     */
    @Query("SELECT pr FROM PeerReview pr WHERE pr.reviewDeadline < :now AND pr.status NOT IN ('approved', 'rejected', 'revision_requested')")
    List<PeerReview> findOverdueReviews(@Param("now") LocalDateTime now);
    
    /**
     * Find reviews by status
     */
    List<PeerReview> findByStatus(PeerReview.ReviewStatus status);
    
    /**
     * Find reviews by type
     */
    List<PeerReview> findByReviewType(PeerReview.ReviewType reviewType);
    
    /**
     * Find reviews with filtering
     */
    @Query("SELECT pr FROM PeerReview pr WHERE " +
           "(:requirementId IS NULL OR pr.requirementId = :requirementId) AND " +
           "(:reviewerId IS NULL OR pr.reviewerId = :reviewerId) AND " +
           "(:authorId IS NULL OR pr.authorId = :authorId) AND " +
           "(:status IS NULL OR pr.status = :status) AND " +
           "(:reviewType IS NULL OR pr.reviewType = :reviewType)")
    Page<PeerReview> findWithFilters(@Param("requirementId") UUID requirementId,
                                    @Param("reviewerId") UUID reviewerId,
                                    @Param("authorId") UUID authorId,
                                    @Param("status") PeerReview.ReviewStatus status,
                                    @Param("reviewType") PeerReview.ReviewType reviewType,
                                    Pageable pageable);
    
    /**
     * Search reviews by comments or author/reviewer information
     */
    @Query("SELECT pr FROM PeerReview pr JOIN User reviewer ON pr.reviewerId = reviewer.id JOIN User author ON pr.authorId = author.id WHERE " +
           "LOWER(pr.comments) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(reviewer.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(author.email) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<PeerReview> findBySearch(@Param("search") String search, Pageable pageable);
    
    /**
     * Find reviews that need attention (pending or overdue)
     */
    @Query("SELECT pr FROM PeerReview pr WHERE pr.status IN ('pending', 'in_progress') OR " +
           "(pr.reviewDeadline < :now AND pr.status NOT IN ('approved', 'rejected', 'revision_requested'))")
    List<PeerReview> findReviewsNeedingAttention(@Param("now") LocalDateTime now);
    
    /**
     * Get review statistics
     */
    @Query("SELECT pr.status, COUNT(pr) FROM PeerReview pr GROUP BY pr.status")
    List<Object[]> getReviewCountByStatus();
    
    @Query("SELECT pr.reviewType, COUNT(pr) FROM PeerReview pr GROUP BY pr.reviewType")
    List<Object[]> getReviewCountByType();
    
    @Query("SELECT pr.reviewerId, COUNT(pr) FROM PeerReview pr WHERE pr.status = 'pending' GROUP BY pr.reviewerId")
    List<Object[]> getPendingReviewCountByReviewer();
    
    /**
     * Find reviews with quality ratings
     */
    @Query("SELECT pr FROM PeerReview pr WHERE pr.qualityRating IS NOT NULL ORDER BY pr.qualityRating DESC")
    List<PeerReview> findReviewsWithQualityRatings(Pageable pageable);
    
    /**
     * Find reviews with effort ratings
     */
    @Query("SELECT pr FROM PeerReview pr WHERE pr.effortRating IS NOT NULL ORDER BY pr.effortRating DESC")
    List<PeerReview> findReviewsWithEffortRatings(Pageable pageable);
    
    /**
     * Find reviews created after a date
     */
    @Query("SELECT pr FROM PeerReview pr WHERE pr.createdAt >= :startDate ORDER BY pr.createdAt DESC")
    List<PeerReview> findReviewsCreatedAfter(@Param("startDate") LocalDateTime startDate, Pageable pageable);
    
    /**
     * Find completed reviews within date range
     */
    @Query("SELECT pr FROM PeerReview pr WHERE pr.status IN ('approved', 'rejected', 'revision_requested') AND pr.reviewedAt BETWEEN :startDate AND :endDate")
    List<PeerReview> findCompletedReviewsInRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Get average ratings for requirements
     */
    @Query("SELECT pr.requirementId, AVG(pr.qualityRating), AVG(pr.effortRating) FROM PeerReview pr WHERE pr.qualityRating IS NOT NULL GROUP BY pr.requirementId")
    List<Object[]> getAverageRatingsByRequirement();
}