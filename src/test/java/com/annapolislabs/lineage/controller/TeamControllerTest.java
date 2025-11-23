package com.annapolislabs.lineage.controller;

import com.annapolislabs.lineage.entity.Team;
import com.annapolislabs.lineage.service.TeamService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamControllerTest {

    @Mock
    private TeamService teamService;

    @InjectMocks
    private TeamController teamController;

    private UUID currentUserId;

    @BeforeEach
    void setUp() {
        currentUserId = UUID.randomUUID();
        mockAuthentication(currentUserId);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getTeams_ProjectScopedPageBeyondRange_ReturnsEmptyPage() {
        UUID projectId = UUID.randomUUID();
        List<Team> teams = List.of(
                buildTeam(projectId, "Alpha"),
                buildTeam(projectId, "Beta")
        );

        when(teamService.getTeamsByProject(projectId, currentUserId)).thenReturn(teams);

        ResponseEntity<Page<Team>> response = teamController.getTeams(projectId, null, false, 5, 10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Page<Team> page = response.getBody();
        assertNotNull(page);
        assertTrue(page.getContent().isEmpty());
        assertEquals(0, page.getNumberOfElements());
        assertEquals(teams.size(), page.getTotalElements());
        assertEquals(5, page.getNumber());

        verify(teamService).getTeamsByProject(projectId, currentUserId);
        verify(teamService, never()).searchTeams(any(), any(), any(), any(), any());
    }

    @Test
    void getTeams_ProjectScopedPartialLastPage_ReturnsSlice() {
        UUID projectId = UUID.randomUUID();
        List<Team> teams = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            teams.add(buildTeam(projectId, "Team " + i));
        }

        when(teamService.getTeamsByProject(projectId, currentUserId)).thenReturn(teams);

        ResponseEntity<Page<Team>> response = teamController.getTeams(projectId, null, false, 1, 5);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Page<Team> page = response.getBody();
        assertNotNull(page);
        assertEquals(2, page.getContent().size());
        assertEquals(teams.subList(5, 7), page.getContent());
        assertEquals(7, page.getTotalElements());
        assertEquals(1, page.getNumber());
        assertEquals(2, page.getNumberOfElements());

        verify(teamService).getTeamsByProject(projectId, currentUserId);
        verify(teamService, never()).searchTeams(any(), any(), any(), any(), any());
    }

    private void mockAuthentication(UUID userId) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userId.toString());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    private Team buildTeam(UUID projectId, String name) {
        Team team = new Team();
        team.setId(UUID.randomUUID());
        team.setProjectId(projectId);
        team.setName(name);
        team.setCreatedBy(UUID.randomUUID());
        team.setActive(true);
        return team;
    }
}
