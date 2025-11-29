package com.annapolislabs.lineage.repository;

import com.annapolislabs.lineage.entity.PermissionChange;
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
 * Repository for PermissionChange entities
 */
@Repository
public interface PermissionChangeRepository extends JpaRepository<PermissionChange, UUID> {

    /**
     * Find permission changes by user ID
     */
    List<PermissionChange> findByUserIdAndApproved(UUID userId, boolean approved);
    
    /**
     * Find permission changes by user ID with pagination
     */
    Page<PermissionChange> findByUserIdAndApproved(UUID userId, boolean approved, Pageable pageable);
    
    /**
     * Find permission changes by changed_by user ID
     */
    List<PermissionChange> findByChangedBy(UUID changedBy);
    
    /**
     * Find permission changes by permission key
     */
    List<PermissionChange> findByPermissionKeyAndApproved(String permissionKey, boolean approved);
    
    /**
     * Find permission changes by resource ID
     */
    List<PermissionChange> findByResourceIdAndApproved(UUID resourceId, boolean approved);
    
    /**
     * Find permission changes by change type
     */
    List<PermissionChange> findByChangeTypeAndApproved(PermissionChange.ChangeType changeType, boolean approved);
    
    /**
     * Find pending permission changes (requiring approval)
     */
    @Query("SELECT pc FROM PermissionChange pc WHERE pc.approved = false AND pc.effectiveFrom > :now")
    Page<PermissionChange> findPendingChanges(@Param("now") LocalDateTime now, Pageable pageable);
    
    /**
     * Find expired permission changes
     */
    @Query("SELECT pc FROM PermissionChange pc WHERE pc.temporary = true AND pc.expiresAt < :now")
    List<PermissionChange> findExpiredChanges(@Param("now") LocalDateTime now);
    
    /**
     * Find permission changes with filtering
     */
    @Query("SELECT pc FROM PermissionChange pc WHERE " +
           "(:userId IS NULL OR pc.userId = :userId) AND " +
           "(:changedBy IS NULL OR pc.changedBy = :changedBy) AND " +
           "(:permissionKey IS NULL OR pc.permissionKey = :permissionKey) AND " +
           "(:changeType IS NULL OR pc.changeType = :changeType) AND " +
           "(:approved IS NULL OR pc.approved = :approved)")
    Page<PermissionChange> findWithFilters(@Param("userId") UUID userId,
                                          @Param("changedBy") UUID changedBy,
                                          @Param("permissionKey") String permissionKey,
                                          @Param("changeType") PermissionChange.ChangeType changeType,
                                          @Param("approved") Boolean approved,
                                          Pageable pageable);
    
    /**
     * Search permission changes by reason or context
     */
    @Query("SELECT pc FROM PermissionChange pc WHERE " +
           "LOWER(pc.reason) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<PermissionChange> findBySearch(@Param("search") String search, Pageable pageable);
    
    /**
     * Find high-risk permission changes
     */
    @Query("SELECT pc FROM PermissionChange pc WHERE pc.riskLevel IN ('HIGH', 'CRITICAL') AND pc.approved = false")
    List<PermissionChange> findHighRiskPendingChanges();
    
    /**
     * Find active permission changes (currently in effect)
     */
    @Query("SELECT pc FROM PermissionChange pc WHERE pc.approved = true AND " +
           "(pc.effectiveFrom IS NULL OR pc.effectiveFrom <= :now) AND " +
           "(pc.effectiveUntil IS NULL OR pc.effectiveUntil > :now)")
    List<PermissionChange> findActiveChanges(@Param("now") LocalDateTime now);
    
    /**
     * Get permission change statistics
     */
    @Query("SELECT pc.changeType, COUNT(pc) FROM PermissionChange pc GROUP BY pc.changeType")
    List<Object[]> getChangeCountByType();
    
    @Query("SELECT pc.riskLevel, COUNT(pc) FROM PermissionChange pc GROUP BY pc.riskLevel")
    List<Object[]> getChangeCountByRiskLevel();
    
    @Query("SELECT pc.changedBy, COUNT(pc) FROM PermissionChange pc WHERE pc.createdAt >= :startDate GROUP BY pc.changedBy")
    List<Object[]> getChangeCountByUserSince(@Param("startDate") LocalDateTime startDate);
    
    /**
     * Find permission changes by date range
     */
    @Query("SELECT pc FROM PermissionChange pc WHERE pc.createdAt BETWEEN :startDate AND :endDate ORDER BY pc.createdAt DESC")
    List<PermissionChange> findChangesInRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, Pageable pageable);
    
    /**
     * Find temporary permission changes expiring soon
     */
    @Query("SELECT pc FROM PermissionChange pc WHERE pc.temporary = true AND pc.expiresAt BETWEEN :now AND :expiryThreshold")
    List<PermissionChange> findExpiringSoon(@Param("now") LocalDateTime now, @Param("expiryThreshold") LocalDateTime expiryThreshold);
    
    /**
     * Find permission changes by request source
     */
    List<PermissionChange> findByRequestSourceAndApproved(String requestSource, boolean approved);
    
    /**
     * Get audit trail for specific user
     */
    @Query("SELECT pc FROM PermissionChange pc WHERE pc.userId = :userId ORDER BY pc.createdAt DESC")
    List<PermissionChange> getAuditTrailForUser(@Param("userId") UUID userId, Pageable pageable);
}