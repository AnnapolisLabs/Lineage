package com.annapolislabs.lineage.repository;

import com.annapolislabs.lineage.entity.UserSession;
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
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
    
    // Basic queries
    List<UserSession> findByUserId(UUID userId);
    Optional<UserSession> findBySessionId(String sessionId);
    Optional<UserSession> findByUserIdAndTokenHash(UUID userId, String tokenHash);
    
    // Active session queries
    @Query("SELECT us FROM UserSession us WHERE us.userId = :userId AND us.revoked = false AND us.expiresAt > :now ORDER BY us.lastActivityAt DESC")
    List<UserSession> findActiveUserSessions(@Param("userId") UUID userId, @Param("now") LocalDateTime now);
    
    @Query("SELECT us FROM UserSession us WHERE us.revoked = false AND us.expiresAt > :now ORDER BY us.lastActivityAt DESC")
    List<UserSession> findAllActiveSessions(@Param("now") LocalDateTime now, Pageable pageable);
    
    // Session management
    @Query("SELECT us FROM UserSession us WHERE us.revoked = true OR us.expiresAt <= :now")
    List<UserSession> findExpiredOrRevokedSessions(@Param("now") LocalDateTime now);
    
    @Query("SELECT us FROM UserSession us WHERE us.expiresAt < :cutoffTime")
    List<UserSession> findSessionsExpiredBefore(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    @Query("SELECT us FROM UserSession us WHERE us.revoked = true AND us.revokedAt < :cutoffTime")
    List<UserSession> findRevokedSessionsBefore(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    // Token-based queries
    @Query("SELECT us FROM UserSession us WHERE us.tokenHash = :tokenHash AND us.revoked = false AND us.expiresAt > :now")
    Optional<UserSession> findActiveByTokenHash(@Param("tokenHash") String tokenHash, @Param("now") LocalDateTime now);
    
    @Query("SELECT us FROM UserSession us WHERE us.refreshTokenHash = :refreshTokenHash AND us.revoked = false AND (us.refreshExpiresAt IS NULL OR us.refreshExpiresAt > :now)")
    Optional<UserSession> findActiveByRefreshTokenHash(@Param("refreshTokenHash") String refreshTokenHash, @Param("now") LocalDateTime now);
    
    // Device and IP tracking
    @Query("SELECT us FROM UserSession us WHERE us.ipAddress = :ipAddress AND us.createdAt >= :since")
    List<UserSession> findSessionsByIpAddressSince(@Param("ipAddress") String ipAddress, @Param("since") LocalDateTime since);
    
    @Query("SELECT DISTINCT us.ipAddress FROM UserSession us WHERE us.ipAddress IS NOT NULL AND us.createdAt >= :since")
    List<String> findDistinctActiveIpAddressesSince(@Param("since") LocalDateTime since);
    
    @Query("SELECT DISTINCT us.userAgent FROM UserSession us WHERE us.userAgent IS NOT NULL AND us.createdAt >= :since")
    List<String> findDistinctUserAgentsSince(@Param("since") LocalDateTime since);
    
    // Activity tracking
    @Query("SELECT us FROM UserSession us WHERE us.userId = :userId ORDER BY us.lastActivityAt DESC")
    List<UserSession> findUserSessionsByActivity(@Param("userId") UUID userId);
    
    @Query("SELECT us FROM UserSession us WHERE us.lastActivityAt < :cutoffTime AND us.revoked = false")
    List<UserSession> findStaleActiveSessions(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    // Session limits and enforcement
    @Query("SELECT COUNT(us) FROM UserSession us WHERE us.userId = :userId AND us.revoked = false AND us.expiresAt > :now")
    long countActiveSessionsForUser(@Param("userId") UUID userId, @Param("now") LocalDateTime now);
    
    @Query("SELECT us FROM UserSession us WHERE us.userId = :userId AND us.revoked = false AND us.expiresAt > :now ORDER BY us.createdAt ASC")
    List<UserSession> findActiveUserSessionsOrderedByCreation(@Param("userId") UUID userId, @Param("now") LocalDateTime now);
    
    // Security monitoring
    @Query("SELECT us FROM UserSession us WHERE us.userId = :userId AND us.ipAddress != :currentIpAddress AND us.revoked = false AND us.expiresAt > :now")
    List<UserSession> findSuspiciousSessionsForUser(@Param("userId") UUID userId, @Param("currentIpAddress") String currentIpAddress, @Param("now") LocalDateTime now);
    
    @Query("SELECT us FROM UserSession us WHERE us.ipAddress = :ipAddress AND us.createdAt >= :since AND us.revoked = false")
    List<UserSession> findActiveSessionsFromIpSince(@Param("ipAddress") String ipAddress, @Param("since") LocalDateTime since);
    
    // Cleanup operations
    void deleteByExpiresAtBefore(LocalDateTime expiresAt);
    void deleteByRevokedTrueAndRevokedAtBefore(LocalDateTime revokedAt);
    void deleteByUserIdAndRevokedTrue(UUID userId);
    
    @Query("DELETE FROM UserSession us WHERE us.expiresAt < :cutoffTime OR (us.revoked = true AND us.revokedAt < :cutoffTime)")
    void cleanupOldSessions(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    // Statistics
    @Query("SELECT COUNT(us) FROM UserSession us WHERE us.createdAt >= :startDate")
    long countSessionsCreatedSince(@Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT COUNT(us) FROM UserSession us WHERE us.revoked = false AND us.expiresAt > :now")
    long countActiveSessions(@Param("now") LocalDateTime now);
    
    @Query("SELECT COUNT(DISTINCT us.userId) FROM UserSession us WHERE us.revoked = false AND us.expiresAt > :now")
    long countActiveUsers(@Param("now") LocalDateTime now);
    
    @Query("SELECT us.ipAddress, COUNT(us) FROM UserSession us WHERE us.createdAt >= :since AND us.ipAddress IS NOT NULL GROUP BY us.ipAddress HAVING COUNT(us) >= :threshold ORDER BY COUNT(us) DESC")
    List<Object[]> findSuspiciousIpActivity(@Param("since") LocalDateTime since, @Param("threshold") long threshold);
    
    // Session activity by time period
    @Query("SELECT DATE_TRUNC('hour', us.createdAt), COUNT(us) FROM UserSession us WHERE us.createdAt >= :since GROUP BY DATE_TRUNC('hour', us.createdAt) ORDER BY DATE_TRUNC('hour', us.createdAt)")
    List<Object[]> findSessionActivityByHourSince(@Param("since") LocalDateTime since);
    
    @Query("SELECT DATE_TRUNC('day', us.createdAt), COUNT(us) FROM UserSession us WHERE us.createdAt >= :since GROUP BY DATE_TRUNC('day', us.createdAt) ORDER BY DATE_TRUNC('day', us.createdAt)")
    List<Object[]> findSessionActivityByDaySince(@Param("since") LocalDateTime since);
    
    // Concurrent session detection
    @Query("SELECT us.userId, COUNT(us) FROM UserSession us WHERE us.revoked = false AND us.expiresAt > :now GROUP BY us.userId HAVING COUNT(us) > :limit ORDER BY COUNT(us) DESC")
    List<Object[]> findUsersWithExcessiveSessions(@Param("now") LocalDateTime now, @Param("limit") long limit);
    
    // Force logout operations
    @Query("UPDATE UserSession us SET us.revoked = true, us.revokedAt = :revokedAt, us.revokedReason = :reason WHERE us.userId = :userId AND us.revoked = false")
    void revokeAllUserSessions(@Param("userId") UUID userId, @Param("revokedAt") LocalDateTime revokedAt, @Param("reason") String reason);
    
    @Query("UPDATE UserSession us SET us.revoked = true, us.revokedAt = :revokedAt, us.revokedReason = :reason WHERE us.sessionId = :sessionId")
    void revokeSessionBySessionId(@Param("sessionId") String sessionId, @Param("revokedAt") LocalDateTime revokedAt, @Param("reason") String reason);
}