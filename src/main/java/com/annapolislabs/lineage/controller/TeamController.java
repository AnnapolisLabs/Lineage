package com.annapolislabs.lineage.controller;

import com.annapolislabs.lineage.service.TeamService;
import com.annapolislabs.lineage.security.JwtTokenProvider;
import com.annapolislabs.lineage.repository.UserRepository;
import com.annapolislabs.lineage.entity.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * REST controller for team management operations
 * Provides endpoints for team CRUD operations, member management, and team collaborations
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/teams")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Team Management", description = "Team creation, member management, and collaboration APIs")
public class TeamController {

    @Autowired
    private UserRepository userRepository;
    private final TeamService teamService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    /**
     * Get all teams accessible to the current user
     */
    @GetMapping
    @Operation(
        summary = "Get teams", 
        description = "Retrieve teams accessible to the current user with optional filtering"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Teams retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Page<Team>> getTeams(
            @Parameter(description = "Filter by project ID")
            @RequestParam(required = false) UUID projectId,
            
            @Parameter(description = "Filter by team status")
            @RequestParam(required = false) Boolean active,
            
            @Parameter(description = "Include team members in response")
            @RequestParam(defaultValue = "false") boolean includeMembers,
            
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {
        
        UUID currentUserId = getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        
        if (projectId != null) {
            List<Team> teams = teamService.getTeamsByProject(projectId, currentUserId);
            int start = Math.toIntExact(Math.min((long) page * size, teams.size()));
            int end = Math.toIntExact(Math.min(start + size, teams.size()));
            List<Team> slice = teams.subList(start, end);
            Page<Team> teamPage = new PageImpl<>(slice, pageable, teams.size());
            return ResponseEntity.ok(teamPage);
        } else {
            Page<Team> teams = teamService.searchTeams(null, null, active, pageable, currentUserId);
            return ResponseEntity.ok(teams);
        }
    }

    /**
     * Create a new team
     */
    @PostMapping
    @Operation(
        summary = "Create team", 
        description = "Create a new team for project collaboration"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Team created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
    })
    public ResponseEntity<Team> createTeam(
            @Parameter(description = "Team creation request", required = true)
            @RequestBody Map<String, Object> request) {
        
        UUID currentUserId = getCurrentUserId();
        
        String name = (String) request.get("name");
        String description = (String) request.get("description");
        UUID projectId = UUID.fromString(request.get("project_id").toString());
        @SuppressWarnings("unchecked")
        Map<String, Object> settings = (Map<String, Object>) request.get("settings");
        
        Team team = teamService.createTeam(name, description, projectId, currentUserId, settings);
        
        log.info("Team created successfully: {}", team.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(team);
    }

    /**
     * Get team by ID
     */
    @GetMapping("/{teamId}")
    @Operation(
        summary = "Get team details", 
        description = "Retrieve detailed information about a specific team"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Team details retrieved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Team not found")
    })
    public ResponseEntity<Team> getTeam(
            @Parameter(description = "Team ID", required = true)
            @PathVariable UUID teamId) {
        
        UUID currentUserId = getCurrentUserId();
        Team team = teamService.getTeamById(teamId, currentUserId);
        
        return ResponseEntity.ok(team);
    }

    /**
     * Update team information
     */
    @PutMapping("/{teamId}")
    @Operation(
        summary = "Update team", 
        description = "Update team information and settings"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Team updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Team not found")
    })
    public ResponseEntity<Team> updateTeam(
            @Parameter(description = "Team ID", required = true)
            @PathVariable UUID teamId,
            
            @Parameter(description = "Team update request", required = true)
            @RequestBody Map<String, Object> request) {
        
        UUID currentUserId = getCurrentUserId();
        
        String name = (String) request.get("name");
        String description = (String) request.get("description");
        @SuppressWarnings("unchecked")
        Map<String, Object> settings = (Map<String, Object>) request.get("settings");
        
        Team team = teamService.updateTeam(teamId, name, description, settings, currentUserId);
        
        log.info("Team updated successfully: {}", teamId);
        return ResponseEntity.ok(team);
    }

    /**
     * Delete/deactivate team
     */
    @DeleteMapping("/{teamId}")
    @Operation(
        summary = "Delete team", 
        description = "Deactivate a team (soft delete)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Team deactivated successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Team not found")
    })
    public ResponseEntity<Void> deleteTeam(
            @Parameter(description = "Team ID", required = true)
            @PathVariable UUID teamId) {
        
        UUID currentUserId = getCurrentUserId();
        teamService.deactivateTeam(teamId, currentUserId);
        
        log.info("Team deactivated successfully: {}", teamId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get team members
     */
    @GetMapping("/{teamId}/members")
    @Operation(
        summary = "Get team members", 
        description = "Retrieve all members of a specific team"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Team members retrieved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Team not found")
    })
    public ResponseEntity<List<TeamMember>> getTeamMembers(
            @Parameter(description = "Team ID", required = true)
            @PathVariable UUID teamId) {
        
        UUID currentUserId = getCurrentUserId();
        List<TeamMember> members = teamService.getTeamMembers(teamId, currentUserId);
        
        return ResponseEntity.ok(members);
    }

    /**
     * Invite user to team
     */
    @PostMapping("/{teamId}/members/invite")
    @Operation(
        summary = "Invite user to team", 
        description = "Send an invitation to a user to join the team"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Invitation sent successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Team not found")
    })
    public ResponseEntity<TeamMember> inviteUserToTeam(
            @Parameter(description = "Team ID", required = true)
            @PathVariable UUID teamId,
            
            @Parameter(description = "Invitation request", required = true)
            @RequestBody Map<String, Object> request) {
        
        UUID currentUserId = getCurrentUserId();
        
        String userEmail = (String) request.get("email");
        String roleName = (String) request.get("role");
        String message = (String) request.get("message");
        
        TeamMember.TeamRole role = TeamMember.TeamRole.valueOf(roleName.toUpperCase());
        TeamMember invitation = teamService.inviteUserToTeam(teamId, userEmail, role, currentUserId, message);
        
        log.info("User invited to team successfully: {} -> {}", userEmail, teamId);
        return ResponseEntity.status(HttpStatus.CREATED).body(invitation);
    }

    /**
     * Accept team invitation
     */
    @PostMapping("/invitations/{invitationId}/accept")
    @Operation(
        summary = "Accept team invitation", 
        description = "Accept a pending team invitation"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Invitation accepted successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid invitation"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Invitation not found")
    })
    public ResponseEntity<Map<String, String>> acceptInvitation(
            @Parameter(description = "Invitation ID", required = true)
            @PathVariable UUID invitationId) {
        
        UUID currentUserId = getCurrentUserId();
        teamService.acceptInvitation(invitationId, currentUserId);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Team invitation accepted successfully");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Remove user from team
     */
    @DeleteMapping("/{teamId}/members/{userId}")
    @Operation(
        summary = "Remove team member", 
        description = "Remove a user from the team"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Member removed successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Team or member not found")
    })
    public ResponseEntity<Void> removeUserFromTeam(
            @Parameter(description = "Team ID", required = true)
            @PathVariable UUID teamId,
            
            @Parameter(description = "User ID to remove", required = true)
            @PathVariable UUID userId) {
        
        UUID currentUserId = getCurrentUserId();
        teamService.removeUserFromTeam(teamId, userId, currentUserId);
        
        log.info("User removed from team successfully: {} from {}", userId, teamId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Update team member role
     */
    @PutMapping("/{teamId}/members/{userId}/role")
    @Operation(
        summary = "Update member role", 
        description = "Update the role of a team member"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Member role updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid role"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Team or member not found")
    })
    public ResponseEntity<Map<String, String>> updateMemberRole(
            @Parameter(description = "Team ID", required = true)
            @PathVariable UUID teamId,
            
            @Parameter(description = "User ID", required = true)
            @PathVariable UUID userId,
            
            @Parameter(description = "Role update request", required = true)
            @RequestBody Map<String, Object> request) {
        
        UUID currentUserId = getCurrentUserId();
        
        String roleName = (String) request.get("role");
        TeamMember.TeamRole newRole = TeamMember.TeamRole.valueOf(roleName.toUpperCase());
        
        teamService.updateMemberRole(teamId, userId, newRole, currentUserId);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Member role updated successfully");
        response.put("new_role", newRole.name());
        
        log.info("Member role updated successfully: {} -> {} in team {}", userId, newRole, teamId);
        return ResponseEntity.ok(response);
    }

    /**
     * Search teams
     */
    @GetMapping("/search")
    @Operation(
        summary = "Search teams", 
        description = "Search teams by name, description, or other criteria"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search results retrieved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Page<Team>> searchTeams(
            @Parameter(description = "Search term")
            @RequestParam(required = false) String search,
            
            @Parameter(description = "Filter by project ID")
            @RequestParam(required = false) UUID projectId,
            
            @Parameter(description = "Filter by team status")
            @RequestParam(required = false) Boolean active,
            
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {
        
        UUID currentUserId = getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        
        Page<Team> teams = teamService.searchTeams(search, projectId, active, pageable, currentUserId);
        
        return ResponseEntity.ok(teams);
    }

    /**
     * Get current user ID from authentication context
     */
    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("User not authenticated");
        }
        
        // Extract user ID from authentication details
        Object principal = authentication.getPrincipal();
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            // Principal is UserDetails, extract email and look up user
            String email = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
            return userRepository.findByEmail(email)
                .map(com.annapolislabs.lineage.entity.User::getId)
                .orElseThrow(() -> new SecurityException("User not found in database"));
        } else if (principal instanceof String) {
            try {
                return UUID.fromString((String) principal);
            } catch (IllegalArgumentException e) {
                throw new SecurityException("Invalid user ID in authentication context");
            }
        }
        
        
        throw new SecurityException("Unable to extract user ID from authentication context");
    }
}
