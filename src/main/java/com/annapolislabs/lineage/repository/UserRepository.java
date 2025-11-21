package com.annapolislabs.lineage.repository;

import com.annapolislabs.lineage.entity.User;
import com.annapolislabs.lineage.entity.UserRole;
import com.annapolislabs.lineage.entity.UserStatus;
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
public interface UserRepository extends JpaRepository<User, UUID> {
    
    // Basic queries
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByGlobalRole(UserRole globalRole);
    
    // Security-related queries
    Optional<User> findByEmailVerificationToken(String token);
    Optional<User> findByPasswordResetToken(String token);
    List<User> findByStatus(UserStatus status);
    Page<User> findByStatus(UserStatus status, Pageable pageable);
    
    // Login and account management
    @Query("SELECT u FROM User u WHERE u.lastLoginAt < :cutoffTime AND u.status = 'ACTIVE'")
    List<User> findInactiveUsers(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    @Query("SELECT u FROM User u WHERE u.failedLoginAttempts >= :attempts")
    List<User> findUsersWithFailedLoginAttempts(@Param("attempts") int attempts);
    
    @Query("SELECT u FROM User u WHERE u.lockedUntil IS NOT NULL AND u.lockedUntil > :now")
    List<User> findLockedUsers(@Param("now") LocalDateTime now);
    
    // Email verification
    @Query("SELECT u FROM User u WHERE u.emailVerified = false AND u.emailVerificationExpiry < :expiry")
    List<User> findUsersWithExpiredEmailVerification(@Param("expiry") LocalDateTime expiry);
    
    // User search and filtering
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<User> findBySearchTerm(@Param("search") String search, Pageable pageable);
    
    // Statistics queries
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :startDate")
    long countUsersCreatedAfter(@Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.emailVerified = true")
    long countVerifiedUsers();
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.status = 'ACTIVE'")
    long countActiveUsers();
    
    // Last login queries
    @Query("SELECT u FROM User u WHERE u.lastLoginAt IS NOT NULL ORDER BY u.lastLoginAt DESC")
    List<User> findUsersByLastLoginActivity(Pageable pageable);
    
    // Recent registrations
    @Query("SELECT u FROM User u WHERE u.createdAt >= :startDate ORDER BY u.createdAt DESC")
    List<User> findRecentlyRegisteredUsers(@Param("startDate") LocalDateTime startDate, Pageable pageable);
    
    // Custom query for admin dashboard
    @Query("SELECT u FROM User u WHERE " +
           "(:status IS NULL OR u.status = :status) AND " +
           "(:role IS NULL OR u.globalRole = :role) AND " +
           "(:verified IS NULL OR u.emailVerified = :verified)")
    Page<User> findUsersWithFilters(@Param("status") UserStatus status,
                                   @Param("role") UserRole role,
                                   @Param("verified") Boolean verified,
                                   Pageable pageable);
}
