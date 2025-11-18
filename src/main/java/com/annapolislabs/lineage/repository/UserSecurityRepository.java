package com.annapolislabs.lineage.repository;

import com.annapolislabs.lineage.entity.UserSecurity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSecurityRepository extends JpaRepository<UserSecurity, UUID> {
    
    // Basic queries
    Optional<UserSecurity> findByUserId(UUID userId);
    boolean existsByUserId(UUID userId);
    
    // MFA-related queries
    @Query("SELECT us FROM UserSecurity us WHERE us.mfaEnabled = true")
    List<UserSecurity> findUsersWithMfaEnabled();
    
    @Query("SELECT us FROM UserSecurity us WHERE us.mfaEnabled = false OR us.mfaEnabled IS NULL")
    List<UserSecurity> findUsersWithoutMfa();
    
    @Query("SELECT us FROM UserSecurity us WHERE us.secretKey IS NOT NULL AND us.mfaEnabled = true")
    List<UserSecurity> findUsersWithMfaSetupComplete();
    
    // Security check queries
    @Query("SELECT us FROM UserSecurity us WHERE us.lastSecurityCheck < :cutoffTime")
    List<UserSecurity> findUsersRequiringSecurityCheck(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    @Query("SELECT us FROM UserSecurity us WHERE us.deviceFingerprint = :fingerprint")
    List<UserSecurity> findByDeviceFingerprint(@Param("fingerprint") String fingerprint);
    
    @Query("SELECT us FROM UserSecurity us WHERE us.mfaEnabled = true AND us.mfaEnabledAt < :cutoffTime")
    List<UserSecurity> findOldMfaSetups(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    // Backup codes queries
    @Query("SELECT us FROM UserSecurity us WHERE us.mfaBackupCodes IS NOT NULL AND us.mfaBackupCodes != ''")
    List<UserSecurity> findUsersWithBackupCodes();
    
    @Query("SELECT us FROM UserSecurity us WHERE us.mfaEnabled = true AND (us.mfaBackupCodes IS NULL OR us.mfaBackupCodes = '')")
    List<UserSecurity> findMfaUsersWithoutBackupCodes();
    
    // Security statistics
    @Query("SELECT COUNT(us) FROM UserSecurity us WHERE us.mfaEnabled = true")
    long countUsersWithMfaEnabled();
    
    @Query("SELECT COUNT(us) FROM UserSecurity us WHERE us.createdAt >= :startDate")
    long countSecurityRecordsCreatedAfter(@Param("startDate") LocalDateTime startDate);
    
    // Device management
    @Query("SELECT us.deviceFingerprint FROM UserSecurity us WHERE us.userId = :userId AND us.deviceFingerprint IS NOT NULL")
    List<String> findUserDeviceFingerprints(@Param("userId") UUID userId);
    
    // Cleanup queries
    @Query("DELETE FROM UserSecurity us WHERE us.userId IN (SELECT u.id FROM User u WHERE u.status = 'DEACTIVATED')")
    void deleteSecurityRecordsForDeactivatedUsers();
}