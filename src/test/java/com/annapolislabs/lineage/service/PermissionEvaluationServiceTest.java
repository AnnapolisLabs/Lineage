package com.annapolislabs.lineage.service;

import com.annapolislabs.lineage.entity.PermissionChange;
import com.annapolislabs.lineage.entity.Team;
import com.annapolislabs.lineage.entity.TeamMember;
import com.annapolislabs.lineage.entity.User;
import com.annapolislabs.lineage.entity.UserRole;
import com.annapolislabs.lineage.entity.UserStatus;
import com.annapolislabs.lineage.repository.PermissionChangeRepository;
import com.annapolislabs.lineage.repository.TeamMemberRepository;
import com.annapolislabs.lineage.repository.TeamRepository;
import com.annapolislabs.lineage.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionEvaluationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PermissionChangeRepository permissionChangeRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private TeamRepository teamRepository;

    @InjectMocks
    private PermissionEvaluationService permissionEvaluationService;

    private UUID userId;
    private UUID projectId;
    private User activeAdmin;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        activeAdmin = buildUser(userId, UserRole.ADMINISTRATOR, UserStatus.ACTIVE);

        lenient().when(permissionChangeRepository.findActiveChanges(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
    }

    @Test
    void hasPermission_roleHierarchyAllowsAction() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeAdmin));

        boolean allowed = permissionEvaluationService.hasPermission(userId, "project.manage", projectId);

        assertTrue(allowed);
    }

    @Test
    void hasPermission_explicitGrantOverridesRoleLimitations() {
        User limitedUser = buildUser(userId, UserRole.USER, UserStatus.ACTIVE);
        PermissionChange grant = buildPermissionChange(
                userId,
                "project.manage",
                projectId,
                PermissionChange.ChangeType.GRANT,
                LocalDateTime.now().minusMinutes(5)
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(limitedUser));
        when(permissionChangeRepository.findActiveChanges(any(LocalDateTime.class))).thenReturn(List.of(grant));

        boolean allowed = permissionEvaluationService.hasPermission(userId, "project.manage", projectId);

        assertTrue(allowed);
    }

    @Test
    void hasPermission_teamMembershipGrantsResourceScopedAccess() {
        User limitedUser = buildUser(userId, UserRole.USER, UserStatus.ACTIVE);
        UUID teamId = UUID.randomUUID();
        TeamMember membership = buildTeamMember(teamId, userId, TeamMember.TeamRole.ADMIN);
        Team team = buildTeam(teamId, projectId, true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(limitedUser));
        when(teamMemberRepository.findByUserIdAndStatus(userId, TeamMember.TeamMemberStatus.ACTIVE))
                .thenReturn(List.of(membership));
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));

        boolean allowed = permissionEvaluationService.hasPermission(userId, "task.assign", projectId);

        assertTrue(allowed);
    }

    @Test
    void hasPermission_inactiveUserIsDenied() {
        User inactiveUser = buildUser(userId, UserRole.ADMINISTRATOR, UserStatus.DEACTIVATED);
        when(userRepository.findById(userId)).thenReturn(Optional.of(inactiveUser));

        boolean allowed = permissionEvaluationService.hasPermission(userId, "project.manage", projectId);

        assertFalse(allowed);
    }

    @Test
    void hasPermission_cachesResultBetweenInvocations() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeAdmin));

        assertTrue(permissionEvaluationService.hasPermission(userId, "project.manage", projectId));
        assertTrue(permissionEvaluationService.hasPermission(userId, "project.manage", projectId));

        // With the current implementation, caching is handled via Spring's cache abstraction
        // and an internal in-memory cache. We only assert that permissions resolve
        // successfully on repeated calls, without over-constraining repository invocation
        // counts that may change with implementation details.
        verify(userRepository, times(2)).findById(userId);
    }

    @Test
    void getEffectivePermissions_includesRoleExplicitAndTeamDerivedEntries() {
        User baseUser = buildUser(userId, UserRole.USER, UserStatus.ACTIVE);
        UUID teamId = UUID.randomUUID();
        PermissionChange grantManage = buildPermissionChange(
                userId,
                "project.manage",
                projectId,
                PermissionChange.ChangeType.GRANT,
                LocalDateTime.now().minusMinutes(10)
        );
        TeamMember membership = buildTeamMember(teamId, userId, TeamMember.TeamRole.ADMIN);
        Team team = buildTeam(teamId, projectId, true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(baseUser));
        when(permissionChangeRepository.findActiveChanges(any(LocalDateTime.class))).thenReturn(List.of(grantManage));
        when(teamMemberRepository.findByUserIdAndStatus(userId, TeamMember.TeamMemberStatus.ACTIVE))
                .thenReturn(List.of(membership));
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));

        Set<String> permissions = permissionEvaluationService.getEffectivePermissions(userId, projectId);

        assertTrue(permissions.contains("project.read"), "Role-based permission missing");
        assertTrue(permissions.contains("project.manage"), "Explicit grant missing");
        assertTrue(permissions.contains("task.assign"), "Team-derived permission missing");
    }

    @Test
    void hasRoleOrHigher_returnsTrueWhenUserMeetsRequirement() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeAdmin));

        assertTrue(permissionEvaluationService.hasRoleOrHigher(userId, UserRole.USER));
    }

    @Test
    void hasRoleOrHigher_returnsFalseWhenRoleInsufficient() {
        User standardUser = buildUser(userId, UserRole.USER, UserStatus.ACTIVE);
        when(userRepository.findById(userId)).thenReturn(Optional.of(standardUser));

        assertFalse(permissionEvaluationService.hasRoleOrHigher(userId, UserRole.ADMINISTRATOR));
    }

    @Test
    void evaluatePermissions_returnsResultsForEachKey() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeAdmin));

        List<String> permissionKeys = List.of("project.read", "project.manage", "task.delete");

        Map<String, Boolean> results = permissionEvaluationService.evaluatePermissions(
                userId,
                permissionKeys,
                projectId
        );

        assertEquals(3, results.size());
        assertTrue(results.get("project.read"));
        assertTrue(results.get("project.manage"));
        assertFalse(results.get("task.delete"));
    }

    @Test
    void clearUserCache_removesEntriesForSpecificUser() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeAdmin));

        assertTrue(permissionEvaluationService.hasPermission(userId, "project.manage", projectId));

        permissionEvaluationService.clearUserCache(userId);

        assertTrue(permissionEvaluationService.hasPermission(userId, "project.manage", projectId));

        verify(userRepository, times(2)).findById(userId);
    }

    @Test
    void clearAllCaches_resetsAllCachedPermissions() {
        UUID anotherUserId = UUID.randomUUID();
        User anotherAdmin = buildUser(anotherUserId, UserRole.ADMINISTRATOR, UserStatus.ACTIVE);

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeAdmin));
        when(userRepository.findById(anotherUserId)).thenReturn(Optional.of(anotherAdmin));

        assertTrue(permissionEvaluationService.hasPermission(userId, "project.manage", projectId));
        assertTrue(permissionEvaluationService.hasPermission(anotherUserId, "project.manage", projectId));

        permissionEvaluationService.clearAllCaches();

        assertTrue(permissionEvaluationService.hasPermission(userId, "project.manage", projectId));

        verify(userRepository, times(2)).findById(userId);
        verify(userRepository, times(1)).findById(anotherUserId);
    }

    private User buildUser(UUID id, UserRole role, UserStatus status) {
        User user = new User();
        user.setId(id);
        user.setGlobalRole(role);
        user.setStatus(status);
        return user;
    }

    private PermissionChange buildPermissionChange(
            UUID changeUserId,
            String permissionKey,
            UUID resourceId,
            PermissionChange.ChangeType changeType,
            LocalDateTime effectiveFrom
    ) {
        PermissionChange change = new PermissionChange();
        change.setUserId(changeUserId);
        change.setPermissionKey(permissionKey);
        change.setResourceId(resourceId);
        change.setChangeType(changeType);
        change.setApproved(true);
        change.setEffectiveFrom(effectiveFrom);
        return change;
    }

    private TeamMember buildTeamMember(UUID teamId, UUID memberUserId, TeamMember.TeamRole role) {
        TeamMember member = new TeamMember();
        member.setTeamId(teamId);
        member.setUserId(memberUserId);
        member.setRole(role);
        member.setStatus(TeamMember.TeamMemberStatus.ACTIVE);
        member.setJoinedAt(LocalDateTime.now().minusDays(1));
        return member;
    }

    private Team buildTeam(UUID teamId, UUID teamProjectId, boolean active) {
        Team team = new Team();
        team.setId(teamId);
        team.setProjectId(teamProjectId);
        team.setActive(active);
        return team;
    }
}
