package com.annapolislabs.lineage.repository;

import com.annapolislabs.lineage.entity.Team;
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
 * Repository for Team entities
 */
@Repository
public interface TeamRepository extends JpaRepository<Team, UUID> {

    /**
     * Find teams by project ID
     */
    List<Team> findByProjectIdAndActiveTrue(UUID projectId);
    
    /**
     * Find teams created by a specific user
     */
    List<Team> findByCreatedByAndActiveTrue(UUID createdBy);
    
    /**
     * Find teams by project ID and name (for unique constraint check)
     */
    Optional<Team> findByProjectIdAndName(UUID projectId, String name);
    
    /**
     * Check if team exists by project ID and name
     */
    boolean existsByProjectIdAndName(UUID projectId, String name);
    
    /**
     * Find active teams with pagination
     */
    Page<Team> findByActiveTrue(Pageable pageable);
    
    /**
     * Find inactive teams
     */
    @Query("SELECT t FROM Team t WHERE t.active = false")
    Page<Team> findInactiveTeams(Pageable pageable);
    
    /**
     * Find teams with filtering
     */
    @Query("SELECT t FROM Team t WHERE " +
           "(:projectId IS NULL OR t.projectId = :projectId) AND " +
           "(:createdBy IS NULL OR t.createdBy = :createdBy) AND " +
           "(:active IS NULL OR t.active = :active)")
    Page<Team> findWithFilters(@Param("projectId") UUID projectId,
                              @Param("createdBy") UUID createdBy,
                              @Param("active") Boolean active,
                              Pageable pageable);
    
    /**
     * Search teams by name or description
     */
    @Query("SELECT t FROM Team t WHERE " +
           "LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Team> findBySearchTerm(@Param("search") String search, Pageable pageable);
    
    /**
     * Find teams that require peer review
     */
    @Query("SELECT t FROM Team t WHERE t.requirePeerReview = true AND t.active = true")
    List<Team> findTeamsRequiringPeerReview();
    
    /**
     * Find teams with auto-assign reviewers enabled
     */
    @Query("SELECT t FROM Team t WHERE t.autoAssignReviewers = true AND t.active = true")
    List<Team> findTeamsWithAutoAssignReviewers();
    
    /**
     * Get team statistics
     */
    @Query("SELECT COUNT(t) FROM Team t WHERE t.active = true")
    long countActiveTeams();
    
    @Query("SELECT COUNT(t) FROM Team t WHERE t.createdAt >= :startDate")
    long countTeamsCreatedAfter(@Param("startDate") LocalDateTime startDate);
}