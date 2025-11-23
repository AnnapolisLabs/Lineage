package com.annapolislabs.lineage.repository;

import com.annapolislabs.lineage.entity.TeamMember;
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
 * Repository for TeamMember entities
 */
@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, UUID> {

    /**
     * Find team members by team ID and status
     */
    List<TeamMember> findByTeamIdAndStatus(UUID teamId, TeamMember.TeamMemberStatus status);

    default List<TeamMember> findByTeamIdAndStatusActive(UUID teamId) {
        return findByTeamIdAndStatus(teamId, TeamMember.TeamMemberStatus.ACTIVE);
    }

    /**
     * Find team members by user ID and status
     */
    List<TeamMember> findByUserIdAndStatus(UUID userId, TeamMember.TeamMemberStatus status);

    default List<TeamMember> findByUserIdAndStatusActive(UUID userId) {
        return findByUserIdAndStatus(userId, TeamMember.TeamMemberStatus.ACTIVE);
    }

    /**
     * Find team member by team ID and user ID (for unique constraint check)
     */
    Optional<TeamMember> findByTeamIdAndUserId(UUID teamId, UUID userId);

    /**
     * Check if team member exists by team ID and user ID
     */
    boolean existsByTeamIdAndUserId(UUID teamId, UUID userId);

    /**
     * Find team members by role and status
     */
    List<TeamMember> findByRoleAndStatus(TeamMember.TeamRole role, TeamMember.TeamMemberStatus status);

    default List<TeamMember> findByRoleAndStatusActive(TeamMember.TeamRole role) {
        return findByRoleAndStatus(role, TeamMember.TeamMemberStatus.ACTIVE);
    }

    /**
     * Find pending team members
     */
    @Query("SELECT tm FROM TeamMember tm WHERE tm.status = 'PENDING'")
    Page<TeamMember> findPendingMembers(Pageable pageable);

    /**
     * Find inactive team members
     */
    @Query("SELECT tm FROM TeamMember tm WHERE tm.status = 'INACTIVE'")
    Page<TeamMember> findInactiveMembers(Pageable pageable);

    /**
     * Find team members by team ID with filtering
     */
    @Query("SELECT tm FROM TeamMember tm WHERE " +
           "(:teamId IS NULL OR tm.teamId = :teamId) AND " +
           "(:userId IS NULL OR tm.userId = :userId) AND " +
           "(:role IS NULL OR tm.role = :role) AND " +
           "(:status IS NULL OR tm.status = :status)")
    Page<TeamMember> findWithFilters(@Param("teamId") UUID teamId,
                                    @Param("userId") UUID userId,
                                    @Param("role") TeamMember.TeamRole role,
                                    @Param("status") TeamMember.TeamMemberStatus status,
                                    Pageable pageable);

    /**
     * Find team members invited by a specific user
     */
    List<TeamMember> findByInvitedByAndStatus(UUID invitedBy, TeamMember.TeamMemberStatus status);

    default List<TeamMember> findByInvitedByAndStatusActive(UUID invitedBy) {
        return findByInvitedByAndStatus(invitedBy, TeamMember.TeamMemberStatus.ACTIVE);
    }

    /**
     * Count active members in a team
     */
    @Query("SELECT COUNT(tm) FROM TeamMember tm WHERE tm.teamId = :teamId AND tm.status = 'ACTIVE'")
    long countActiveMembersInTeam(@Param("teamId") UUID teamId);

    /**
     * Get team members with contributions scores
     */
    @Query("SELECT tm FROM TeamMember tm WHERE tm.teamId = :teamId AND tm.status = 'ACTIVE' " +
           "ORDER BY tm.contributionScore DESC")
    List<TeamMember> findTopContributorsInTeam(@Param("teamId") UUID teamId, Pageable pageable);

    /**
     * Find team members by activity status
     */
    @Query("SELECT tm FROM TeamMember tm WHERE tm.lastActivityAt < :cutoffTime AND tm.status = 'ACTIVE'")
    List<TeamMember> findInactiveMembers(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Find team owners/admins for a specific team
     */
    @Query("SELECT tm FROM TeamMember tm WHERE tm.teamId = :teamId AND tm.role IN ('OWNER', 'ADMIN') AND tm.status = 'ACTIVE'")
    List<TeamMember> findTeamAdmins(@Param("teamId") UUID teamId);

    /**
     * Search team members by user information
     */
    @Query("SELECT tm FROM TeamMember tm JOIN User u ON tm.userId = u.id WHERE " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<TeamMember> findByUserSearch(@Param("search") String search, Pageable pageable);

    /**
     * Get member statistics
     */
    @Query("SELECT tm.role, COUNT(tm) FROM TeamMember tm WHERE tm.status = 'ACTIVE' GROUP BY tm.role")
    List<Object[]> getMemberCountByRole();

    @Query("SELECT tm.teamId, COUNT(tm) FROM TeamMember tm WHERE tm.status = 'ACTIVE' GROUP BY tm.teamId")
    List<Object[]> getMemberCountByTeam();

    /**
     * Find recently joined members
     */
    @Query("SELECT tm FROM TeamMember tm WHERE tm.joinedAt >= :startDate ORDER BY tm.joinedAt DESC")
    List<TeamMember> findRecentlyJoined(@Param("startDate") LocalDateTime startDate, Pageable pageable);
}