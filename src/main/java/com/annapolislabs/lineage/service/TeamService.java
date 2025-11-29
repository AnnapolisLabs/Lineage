package com.annapolislabs.lineage.service;

import com.annapolislabs.lineage.entity.*;
import com.annapolislabs.lineage.repository.*;
import com.annapolislabs.lineage.security.SecurityAuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for team management operations
 * Handles team creation, membership management, and team operations
 */
@Slf4j
@Service
public class TeamService {

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PermissionEvaluationService permissionEvaluationService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private SecurityAuditService securityAuditService;

    /**
     * Create a new team
     */
    @Transactional
    public Team createTeam(String name, String description, UUID projectId, UUID createdBy, 
                          Map<String, Object> settings) {
        log.info("Creating team '{}' for project {} by user {}", name, projectId, createdBy);

        // Validate inputs
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("Team name is required");
        }
        if (projectId == null) {
            throw new IllegalArgumentException("Project ID is required");
        }
        if (createdBy == null) {
            throw new IllegalArgumentException("Creator user ID is required");
        }

        // Check if user has permission to create teams
        if (!permissionEvaluationService.hasPermission(createdBy, "team.manage", projectId)) {
            throw new SecurityException("User does not have permission to create teams for this project");
        }

        // Check if team name already exists for the project
        if (teamRepository.existsByProjectIdAndName(projectId, name)) {
            throw new IllegalArgumentException("Team name already exists for this project");
        }

        // Validate user exists and is active
        User creator = userRepository.findById(createdBy)
                .orElseThrow(() -> new IllegalArgumentException("Creator user not found: " + createdBy));
        if (creator.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Creator user is not active");
        }

        // Create team
        Team team = new Team(name, description, projectId, createdBy);
        if (settings != null) {
            team.setSettings(settings);
            // Extract team-specific settings
            if (settings.containsKey("autoAssignReviewers")) {
                team.setAutoAssignReviewers((Boolean) settings.get("autoAssignReviewers"));
            }
            if (settings.containsKey("requirePeerReview")) {
                team.setRequirePeerReview((Boolean) settings.get("requirePeerReview"));
            }
            if (settings.containsKey("maxMembers")) {
                team.setMaxMembers((Integer) settings.get("maxMembers"));
            }
        }

        // Save team
        team = teamRepository.save(team);

        // Add creator as team owner
        TeamMember ownerMembership = new TeamMember(team.getId(), createdBy, TeamMember.TeamRole.OWNER);
        ownerMembership.setInvitedBy(createdBy);
        teamMemberRepository.save(ownerMembership);

        // Audit log
        securityAuditService.logEvent("TEAM_CREATED", createdBy, "TEAM", team.getId(),
                Map.of("team_name", name, "project_id", projectId, "settings", settings));

        log.info("Team '{}' created successfully with ID {}", name, team.getId());
        return team;
    }

    /**
     * Get team by ID with member information
     */
    public Team getTeamById(UUID teamId, UUID requestingUserId) {
        log.debug("Getting team {} for user {}", teamId, requestingUserId);

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found: " + teamId));

        // Check if user has permission to view team
        if (!permissionEvaluationService.hasPermission(requestingUserId, "team.read", team.getProjectId())) {
            throw new SecurityException("User does not have permission to view this team");
        }

        return team;
    }

    /**
     * Get teams for a project
     */
    public List<Team> getTeamsByProject(UUID projectId, UUID requestingUserId) {
        log.debug("Getting teams for project {} by user {}", projectId, requestingUserId);

        // Check if user has permission to view teams for this project
        if (!permissionEvaluationService.hasPermission(requestingUserId, "team.read", projectId)) {
            throw new SecurityException("User does not have permission to view teams for this project");
        }

        return teamRepository.findByProjectIdAndActiveTrue(projectId);
    }

    /**
     * Update team information
     */
    @Transactional
    public Team updateTeam(UUID teamId, String name, String description, Map<String, Object> settings, 
                          UUID requestingUserId) {
        log.info("Updating team {} by user {}", teamId, requestingUserId);

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found: " + teamId));

        // Check if user has permission to manage team
        TeamMember membership = teamMemberRepository.findByTeamIdAndUserId(teamId, requestingUserId)
                .orElseThrow(() -> new SecurityException("User is not a member of this team"));
        
        if (!membership.canManage()) {
            throw new SecurityException("User does not have permission to manage this team");
        }

        // Update name if changed and check uniqueness
        if (StringUtils.hasText(name) && !name.equals(team.getName())) {
            if (teamRepository.existsByProjectIdAndName(team.getProjectId(), name)) {
                throw new IllegalArgumentException("Team name already exists for this project");
            }
            team.setName(name);
        }

        // Update description
        if (StringUtils.hasText(description)) {
            team.setDescription(description);
        }

        // Update settings
        if (settings != null) {
            team.setSettings(settings);
            if (settings.containsKey("autoAssignReviewers")) {
                team.setAutoAssignReviewers((Boolean) settings.get("autoAssignReviewers"));
            }
            if (settings.containsKey("requirePeerReview")) {
                team.setRequirePeerReview((Boolean) settings.get("requirePeerReview"));
            }
            if (settings.containsKey("maxMembers")) {
                team.setMaxMembers((Integer) settings.get("maxMembers"));
            }
        }

        team = teamRepository.save(team);

        // Audit log
        securityAuditService.logEvent("TEAM_UPDATED", requestingUserId, "TEAM", teamId,
                Map.of("name", name, "description", description, "settings", settings));

        log.info("Team {} updated successfully", teamId);
        return team;
    }

    /**
     * Invite user to team
     */
    @Transactional
    public TeamMember inviteUserToTeam(UUID teamId, String userEmail, TeamMember.TeamRole role, 
                                      UUID invitedBy, String message) {
        log.info("Inviting user {} to team {} with role {} by user {}", userEmail, teamId, role, invitedBy);

        // Validate inputs
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found: " + teamId));

        if (!team.isActive()) {
            throw new IllegalArgumentException("Cannot invite users to inactive team");
        }

        if (!team.canAddMember()) {
            throw new IllegalArgumentException("Team is at maximum capacity");
        }

        // Check if inviter has permission
        TeamMember inviterMembership = teamMemberRepository.findByTeamIdAndUserId(teamId, invitedBy)
                .orElseThrow(() -> new SecurityException("Inviter is not a member of this team"));
        
        if (!inviterMembership.canInvite()) {
            throw new SecurityException("User does not have permission to invite to this team");
        }

        // Find or validate user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + userEmail));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Cannot invite inactive user");
        }

        // Check if user is already a member
        Optional<TeamMember> existingMembership = teamMemberRepository.findByTeamIdAndUserId(teamId, user.getId());
        if (existingMembership.isPresent()) {
            throw new IllegalArgumentException("User is already a member of this team");
        }

        // Create membership
        TeamMember membership = new TeamMember(teamId, user.getId(), role);
        membership.setStatus(TeamMember.TeamMemberStatus.PENDING);
        membership.setInvitedBy(invitedBy);
        
        membership = teamMemberRepository.save(membership);

        // Send invitation email
        try {
            emailService.sendTeamInvitation(user.getEmail(), team.getName(), role.getDisplayName(), 
                                          message, teamId.toString(), membership.getId().toString());
        } catch (Exception e) {
            log.error("Failed to send team invitation email to {}", userEmail, e);
            // Don't fail the operation if email fails
        }

        // Audit log
        securityAuditService.logEvent("USER_INVITED_TO_TEAM", invitedBy, "TEAM", teamId,
                Map.of("invited_user_email", userEmail, "role", role.name(), "membership_id", membership.getId()));

        log.info("User {} invited to team {} successfully", userEmail, teamId);
        return membership;
    }

    /**
     * Accept team invitation
     */
    @Transactional
    public void acceptInvitation(UUID membershipId, UUID userId) {
        log.info("Accepting team invitation {} for user {}", membershipId, userId);

        TeamMember membership = teamMemberRepository.findById(membershipId)
                .orElseThrow(() -> new IllegalArgumentException("Team invitation not found: " + membershipId));

        if (!membership.getUserId().equals(userId)) {
            throw new SecurityException("Team invitation does not belong to this user");
        }

        if (membership.getStatus() != TeamMember.TeamMemberStatus.PENDING) {
            throw new IllegalArgumentException("Team invitation is not in pending status");
        }

        // Activate membership
        membership.setStatus(TeamMember.TeamMemberStatus.ACTIVE);
        membership.setLastActivityAt(LocalDateTime.now());
        teamMemberRepository.save(membership);

        // Audit log
        securityAuditService.logEvent("TEAM_INVITATION_ACCEPTED", userId, "TEAM", membership.getTeamId(),
                Map.of("membership_id", membershipId));

        log.info("Team invitation {} accepted successfully", membershipId);
    }

    /**
     * Remove user from team
     */
    @Transactional
    public void removeUserFromTeam(UUID teamId, UUID userIdToRemove, UUID requestingUserId) {
        log.info("Removing user {} from team {} by user {}", userIdToRemove, teamId, requestingUserId);

        // Validate inputs
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found: " + teamId));

        TeamMember membershipToRemove = teamMemberRepository.findByTeamIdAndUserId(teamId, userIdToRemove)
                .orElseThrow(() -> new IllegalArgumentException("User is not a member of this team"));

        // Check permissions
        TeamMember requestingMembership = teamMemberRepository.findByTeamIdAndUserId(teamId, requestingUserId)
                .orElseThrow(() -> new SecurityException("Requesting user is not a member of this team"));

        if (!requestingMembership.canRemove(membershipToRemove)) {
            throw new SecurityException("User does not have permission to remove this member");
        }

        // Cannot remove team owners unless there are other owners
        if (membershipToRemove.getRole() == TeamMember.TeamRole.OWNER) {
            long ownerCount = teamMemberRepository.countActiveMembersInTeam(teamId);
            if (ownerCount <= 1) {
                throw new IllegalArgumentException("Cannot remove the last team owner");
            }
        }

        // Deactivate membership
        membershipToRemove.setStatus(TeamMember.TeamMemberStatus.INACTIVE);
        teamMemberRepository.save(membershipToRemove);

        // Audit log
        securityAuditService.logEvent("USER_REMOVED_FROM_TEAM", requestingUserId, "TEAM", teamId,
                Map.of("removed_user_id", userIdToRemove, "role", membershipToRemove.getRole().name()));

        log.info("User {} removed from team {} successfully", userIdToRemove, teamId);
    }

    /**
     * Get team members
     */
    public List<TeamMember> getTeamMembers(UUID teamId, UUID requestingUserId) {
        log.debug("Getting team members for team {} by user {}", teamId, requestingUserId);

        // Check permission
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found: " + teamId));

        if (!permissionEvaluationService.hasPermission(requestingUserId, "team.read", team.getProjectId())) {
            throw new SecurityException("User does not have permission to view team members");
        }

        // Load active team members
        List<TeamMember> members = teamMemberRepository.findByTeamIdAndStatusActive(teamId);

        if (members.isEmpty()) {
            return members;
        }

        // Hydrate transient `user` field so API consumers receive
        // embedded user details (name, email, etc.) along with
        // membership metadata. This keeps the database schema
        // normalised (only userId is stored on TeamMember) while
        // still providing a convenient denormalised view over HTTP.
        Set<UUID> userIds = members.stream()
                .map(TeamMember::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (!userIds.isEmpty()) {
            Map<UUID, User> usersById = userRepository.findAllById(userIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> u));

            for (TeamMember member : members) {
                User user = usersById.get(member.getUserId());
                member.setUser(user);
            }
        }

        return members;
    }

    /**
     * Update team member role
     */
    @Transactional
    public void updateMemberRole(UUID teamId, UUID userId, TeamMember.TeamRole newRole, UUID requestingUserId) {
        log.info("Updating role for user {} in team {} to {} by user {}", userId, teamId, newRole, requestingUserId);

        // Validate inputs
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found: " + teamId));

        TeamMember membership = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new IllegalArgumentException("User is not a member of this team"));

        TeamMember requestingMembership = teamMemberRepository.findByTeamIdAndUserId(teamId, requestingUserId)
                .orElseThrow(() -> new SecurityException("Requesting user is not a member of this team"));

        // Check permissions
        if (!requestingMembership.canManage()) {
            throw new SecurityException("User does not have permission to manage team members");
        }

        // Cannot change owner role unless there are other owners
        if (membership.getRole() == TeamMember.TeamRole.OWNER && newRole != TeamMember.TeamRole.OWNER) {
            long ownerCount = teamMemberRepository.countActiveMembersInTeam(teamId);
            if (ownerCount <= 1) {
                throw new IllegalArgumentException("Cannot demote the last team owner");
            }
        }

        // Update role
        TeamMember.TeamRole oldRole = membership.getRole();
        membership.setRole(newRole);
        teamMemberRepository.save(membership);

        // Audit log
        securityAuditService.logEvent("TEAM_MEMBER_ROLE_UPDATED", requestingUserId, "TEAM", teamId,
                Map.of("user_id", userId, "old_role", oldRole.name(), "new_role", newRole.name()));

        log.info("Role updated successfully for user {} in team {} from {} to {}", userId, teamId, oldRole, newRole);
    }

    /**
     * Deactivate team
     */
    @Transactional
    public void deactivateTeam(UUID teamId, UUID requestingUserId) {
        log.info("Deactivating team {} by user {}", teamId, requestingUserId);

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found: " + teamId));

        // Check permission
        TeamMember membership = teamMemberRepository.findByTeamIdAndUserId(teamId, requestingUserId)
                .orElseThrow(() -> new SecurityException("User is not a member of this team"));
        
        if (!membership.canManage()) {
            throw new SecurityException("User does not have permission to deactivate this team");
        }

        // Deactivate team
        team.setActive(false);
        teamRepository.save(team);

        // Deactivate all memberships
        List<TeamMember> members = teamMemberRepository.findByTeamIdAndStatusActive(teamId);
        for (TeamMember member : members) {
            member.setStatus(TeamMember.TeamMemberStatus.INACTIVE);
        }
        teamMemberRepository.saveAll(members);

        // Audit log
        securityAuditService.logEvent("TEAM_DEACTIVATED", requestingUserId, "TEAM", teamId,
                Map.of("team_name", team.getName()));

        log.info("Team {} deactivated successfully", teamId);
    }

    /**
     * Search teams
     */
    public Page<Team> searchTeams(String searchTerm, UUID projectId, Boolean active, Pageable pageable, UUID requestingUserId) {
        log.debug("Searching teams with term '{}' by user {}", searchTerm, requestingUserId);

        // If project ID is provided, check permission for that project
        if (projectId != null) {
            if (!permissionEvaluationService.hasPermission(requestingUserId, "team.read", projectId)) {
                throw new SecurityException("User does not have permission to search teams for this project");
            }
        }

        // Build filter criteria
        if (StringUtils.hasText(searchTerm)) {
            return teamRepository.findBySearchTerm(searchTerm, pageable);
        } else {
            return teamRepository.findWithFilters(projectId, null, active, pageable);
        }
    }
}