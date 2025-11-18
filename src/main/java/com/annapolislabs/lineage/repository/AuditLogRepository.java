package com.annapolislabs.lineage.repository;

import com.annapolislabs.lineage.entity.AuditLog;
import com.annapolislabs.lineage.entity.AuditSeverity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    
    // Basic queries
    List<AuditLog> findByUserId(UUID userId);
    List<AuditLog> findByAction(String action);
    List<AuditLog> findByResource(String resource);
    List<AuditLog> findByResourceId(String resourceId);
    List<AuditLog> findBySeverity(AuditSeverity severity);
    
    // Date range queries
    @Query("SELECT al FROM AuditLog al WHERE al.createdAt BETWEEN :startDate AND :endDate")
    List<AuditLog> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT al FROM AuditLog al WHERE al.userId = :userId AND al.createdAt BETWEEN :startDate AND :endDate")
    List<AuditLog> findByUserIdAndDateRange(@Param("userId") UUID userId, 
                                           @Param("startDate") LocalDateTime startDate, 
                                           @Param("endDate") LocalDateTime endDate);
    
    // Severity-based queries
    @Query("SELECT al FROM AuditLog al WHERE al.severity IN :severities ORDER BY al.createdAt DESC")
    List<AuditLog> findBySeverityInOrderByCreatedAtDesc(@Param("severities") List<AuditSeverity> severities);
    
    @Query("SELECT al FROM AuditLog al WHERE al.severity = 'CRITICAL' AND al.createdAt >= :since")
    List<AuditLog> findRecentCriticalEvents(@Param("since") LocalDateTime since);
    
    @Query("SELECT al FROM AuditLog al WHERE al.severity IN ('WARNING', 'ERROR', 'CRITICAL') AND al.createdAt >= :since")
    List<AuditLog> findRecentSecurityEvents(@Param("since") LocalDateTime since);
    
    // Search and filtering
    @Query("SELECT al FROM AuditLog al WHERE " +
           "LOWER(al.action) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(al.resource) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(al.resourceId) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<AuditLog> findBySearchTerm(@Param("search") String search, Pageable pageable);
    
    @Query("SELECT al FROM AuditLog al WHERE " +
           "(:userId IS NULL OR al.userId = :userId) AND " +
           "(:action IS NULL OR al.action = :action) AND " +
           "(:resource IS NULL OR al.resource = :resource) AND " +
           "(:severity IS NULL OR al.severity = :severity) AND " +
           "(:startDate IS NULL OR al.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR al.createdAt <= :endDate)")
    Page<AuditLog> findWithFilters(@Param("userId") UUID userId,
                                  @Param("action") String action,
                                  @Param("resource") String resource,
                                  @Param("severity") AuditSeverity severity,
                                  @Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate,
                                  Pageable pageable);
    
    // IP-based queries
    @Query("SELECT al FROM AuditLog al WHERE al.ipAddress = :ipAddress ORDER BY al.createdAt DESC")
    List<AuditLog> findByIpAddress(@Param("ipAddress") String ipAddress);
    
    @Query("SELECT DISTINCT al.ipAddress FROM AuditLog al WHERE al.ipAddress IS NOT NULL")
    List<String> findDistinctIpAddresses();
    
    // Action-based aggregation
    @Query("SELECT al.action, COUNT(al) FROM AuditLog al WHERE al.createdAt >= :since GROUP BY al.action ORDER BY COUNT(al) DESC")
    List<Object[]> findActionCountsSince(@Param("since") LocalDateTime since);
    
    @Query("SELECT al.severity, COUNT(al) FROM AuditLog al WHERE al.createdAt >= :since GROUP BY al.severity")
    List<Object[]> findSeverityCountsSince(@Param("since") LocalDateTime since);
    
    // User activity aggregation
    @Query("SELECT al.userId, COUNT(al) FROM AuditLog al WHERE al.createdAt >= :since GROUP BY al.userId ORDER BY COUNT(al) DESC")
    List<Object[]> findUserActivityCountsSince(@Param("since") LocalDateTime since);
    
    // Recent activity
    @Query("SELECT al FROM AuditLog al ORDER BY al.createdAt DESC")
    List<AuditLog> findRecentActivity(Pageable pageable);
    
    @Query("SELECT al FROM AuditLog al WHERE al.userId = :userId ORDER BY al.createdAt DESC")
    List<AuditLog> findUserRecentActivity(@Param("userId") UUID userId, Pageable pageable);
    
    List<AuditLog> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<AuditLog> findByUserIdAndActionOrderByCreatedAtDesc(UUID userId, String action);

    // Cleanup and maintenance
    @Query("DELETE FROM AuditLog al WHERE al.createdAt < :cutoffDate")
    void deleteOldLogs(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    @Query("DELETE FROM AuditLog al WHERE al.severity = 'INFO' AND al.createdAt < :cutoffDate")
    void deleteOldInfoLogs(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    // Statistics
    @Query("SELECT COUNT(al) FROM AuditLog al WHERE al.createdAt >= :startDate")
    long countLogsSince(@Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT COUNT(al) FROM AuditLog al WHERE al.severity = :severity AND al.createdAt >= :startDate")
    long countLogsBySeveritySince(@Param("severity") AuditSeverity severity, @Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT COUNT(DISTINCT al.userId) FROM AuditLog al WHERE al.createdAt >= :startDate")
    long countActiveUsersSince(@Param("startDate") LocalDateTime startDate);
    
    // Security event monitoring
    @Query("SELECT al FROM AuditLog al WHERE al.action IN :securityActions AND al.createdAt >= :since ORDER BY al.createdAt DESC")
    List<AuditLog> findSecurityEventsSince(@Param("securityActions") List<String> securityActions, 
                                         @Param("since") LocalDateTime since);
    
    // Failed login monitoring
    @Query("SELECT al FROM AuditLog al WHERE al.action = 'LOGIN_FAILED' AND al.createdAt >= :since")
    List<AuditLog> findFailedLoginsSince(@Param("since") LocalDateTime since);
    
    @Query("SELECT al.ipAddress, COUNT(al) FROM AuditLog al WHERE al.action = 'LOGIN_FAILED' AND al.createdAt >= :since GROUP BY al.ipAddress HAVING COUNT(al) >= :threshold ORDER BY COUNT(al) DESC")
    List<Object[]> findSuspiciousIpActivity(@Param("since") LocalDateTime since, @Param("threshold") long threshold);
}