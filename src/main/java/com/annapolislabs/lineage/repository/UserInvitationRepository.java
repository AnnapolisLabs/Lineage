package com.annapolislabs.lineage.repository;

import com.annapolislabs.lineage.entity.UserInvitations;
import com.annapolislabs.lineage.entity.InvitationStatus;
import com.annapolislabs.lineage.entity.ProjectRole;
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

@Repository
public interface UserInvitationRepository extends JpaRepository<UserInvitations, UUID> {
    
    // Basic queries
    Optional<UserInvitations> findByToken(String token);
    List<UserInvitations> findByInvitedBy(UUID invitedBy);
    List<UserInvitations> findByEmail(String email);
    List<UserInvitations> findByProjectId(UUID projectId);
    
    // Status-based queries
    List<UserInvitations> findByStatus(InvitationStatus status);
    @Query("SELECT ui FROM UserInvitations ui WHERE ui.status = 'PENDING' AND ui.expiryDate > :now")
    List<UserInvitations> findPendingValidInvitations(@Param("now") LocalDateTime now);
    
    @Query("SELECT ui FROM UserInvitations ui WHERE ui.status = 'PENDING' AND ui.expiryDate <= :now")
    List<UserInvitations> findExpiredInvitations(@Param("now") LocalDateTime now);
    
    // Date-based queries
    @Query("SELECT ui FROM UserInvitations ui WHERE ui.createdAt >= :startDate ORDER BY ui.createdAt DESC")
    List<UserInvitations> findInvitationsCreatedSince(@Param("startDate") LocalDateTime startDate, Pageable pageable);
    
    @Query("SELECT ui FROM UserInvitations ui WHERE ui.invitedBy = :invitedBy AND ui.createdAt >= :startDate")
    List<UserInvitations> findInvitationsByInviterSince(@Param("invitedBy") UUID invitedBy, @Param("startDate") LocalDateTime startDate);
    
    // Email and project filtering
    @Query("SELECT ui FROM UserInvitations ui WHERE ui.email = :email AND ui.status = 'PENDING' AND ui.expiryDate > :now")
    List<UserInvitations> findPendingValidInvitationsByEmail(@Param("email") String email, @Param("now") LocalDateTime now);
    
    @Query("SELECT ui FROM UserInvitations ui WHERE ui.projectId = :projectId AND ui.status = 'PENDING'")
    List<UserInvitations> findPendingInvitationsByProject(@Param("projectId") UUID projectId);
    
    @Query("SELECT ui FROM UserInvitations ui WHERE ui.projectId = :projectId AND ui.projectRole = :role")
    List<UserInvitations> findInvitationsByProjectAndRole(@Param("projectId") UUID projectId, @Param("role") ProjectRole role);
    
    // User invitation tracking
    @Query("SELECT ui FROM UserInvitations ui WHERE ui.email = :email ORDER BY ui.createdAt DESC")
    List<UserInvitations> findUserInvitationHistory(@Param("email") String email, Pageable pageable);
    
    @Query("SELECT ui FROM UserInvitations ui WHERE ui.invitedBy = :invitedBy ORDER BY ui.createdAt DESC")
    List<UserInvitations> findInviterHistory(@Param("invitedBy") UUID invitedBy, Pageable pageable);
    
    // Token validation
    @Query("SELECT ui FROM UserInvitations ui WHERE ui.token = :token AND ui.status = 'PENDING' AND ui.expiryDate > :now")
    Optional<UserInvitations> findValidInvitationByToken(@Param("token") String token, @Param("now") LocalDateTime now);
    
    // Cleanup operations
    @Query("DELETE FROM UserInvitations ui WHERE ui.expiryDate < :cutoffDate OR ui.status = 'EXPIRED'")
    void deleteExpiredInvitations(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    @Query("DELETE FROM UserInvitations ui WHERE ui.status = 'CANCELLED' AND ui.createdAt < :cutoffDate")
    void deleteOldCancelledInvitations(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    @Query("DELETE FROM UserInvitations ui WHERE ui.invitedBy = :invitedBy AND ui.status = 'PENDING' AND ui.expiryDate < :cutoffDate")
    void deleteExpiredInvitationsByInviter(@Param("invitedBy") UUID invitedBy, @Param("cutoffDate") LocalDateTime cutoffDate);
    
    // Statistics and analytics
    @Query("SELECT COUNT(ui) FROM UserInvitations ui WHERE ui.createdAt >= :startDate")
    long countInvitationsCreatedSince(@Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT COUNT(ui) FROM UserInvitations ui WHERE ui.status = 'ACCEPTED' AND ui.createdAt >= :startDate")
    long countAcceptedInvitationsSince(@Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT COUNT(ui) FROM UserInvitations ui WHERE ui.invitedBy = :invitedBy AND ui.createdAt >= :startDate")
    long countInvitationsSentByUserSince(@Param("invitedBy") UUID invitedBy, @Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT ui.projectRole, COUNT(ui) FROM UserInvitations ui WHERE ui.createdAt >= :startDate GROUP BY ui.projectRole ORDER BY COUNT(ui) DESC")
    List<Object[]> findInvitationRoleDistributionSince(@Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT ui.invitedBy, COUNT(ui) FROM UserInvitations ui WHERE ui.createdAt >= :startDate GROUP BY ui.invitedBy ORDER BY COUNT(ui) DESC")
    List<Object[]> findMostActiveInvitersSince(@Param("startDate") LocalDateTime startDate, Pageable pageable);
    
    // Acceptance rate analysis
    @Query("SELECT DATE_TRUNC('day', ui.createdAt), COUNT(ui), COUNT(CASE WHEN ui.status = 'ACCEPTED' THEN 1 END) FROM UserInvitations ui WHERE ui.createdAt >= :since GROUP BY DATE_TRUNC('day', ui.createdAt) ORDER BY DATE_TRUNC('day', ui.createdAt)")
    List<Object[]> findDailyInvitationStatsSince(@Param("since") LocalDateTime since);
    
    // Project invitation analytics
    @Query("SELECT ui.projectId, COUNT(ui) FROM UserInvitations ui WHERE ui.createdAt >= :since AND ui.projectId IS NOT NULL GROUP BY ui.projectId ORDER BY COUNT(ui) DESC")
    List<Object[]> findMostInvitedProjectsSince(@Param("since") LocalDateTime since, Pageable pageable);
    
    // Bulk operations
    @Query("UPDATE UserInvitations ui SET ui.status = 'EXPIRED' WHERE ui.status = 'PENDING' AND ui.expiryDate < :now")
    void expirePendingInvitations(@Param("now") LocalDateTime now);
    
    @Query("UPDATE UserInvitations ui SET ui.status = 'CANCELLED' WHERE ui.id = :invitationId AND ui.invitedBy = :invitedBy AND ui.status = 'PENDING'")
    int cancelInvitation(@Param("invitationId") UUID invitationId, @Param("invitedBy") UUID invitedBy);
    
    @Query("UPDATE UserInvitations ui SET ui.status = 'ACCEPTED', ui.acceptedAt = :acceptedAt WHERE ui.id = :invitationId AND ui.status = 'PENDING'")
    int acceptInvitation(@Param("invitationId") UUID invitationId, @Param("acceptedAt") LocalDateTime acceptedAt);
    
    // Search and filtering
    @Query("SELECT ui FROM UserInvitations ui WHERE " +
           "(:invitedBy IS NULL OR ui.invitedBy = :invitedBy) AND " +
           "(:email IS NULL OR LOWER(ui.email) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
           "(:projectId IS NULL OR ui.projectId = :projectId) AND " +
           "(:status IS NULL OR ui.status = :status) AND " +
           "(:projectRole IS NULL OR ui.projectRole = :projectRole)")
    Page<UserInvitations> findWithFilters(@Param("invitedBy") UUID invitedBy,
                                         @Param("email") String email,
                                         @Param("projectId") UUID projectId,
                                         @Param("status") InvitationStatus status,
                                         @Param("projectRole") ProjectRole projectRole,
                                         Pageable pageable);
    
    // Invitation validation for duplicate checks
    @Query("SELECT ui FROM UserInvitations ui WHERE ui.email = :email AND ui.projectId = :projectId AND ui.status = 'PENDING' AND ui.expiryDate > :now")
    List<UserInvitations> findActiveInvitationsForEmailAndProject(@Param("email") String email, @Param("projectId") UUID projectId, @Param("now") LocalDateTime now);
}