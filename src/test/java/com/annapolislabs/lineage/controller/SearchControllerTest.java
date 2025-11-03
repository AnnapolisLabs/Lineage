package com.annapolislabs.lineage.controller;

import com.annapolislabs.lineage.dto.response.RequirementResponse;
import com.annapolislabs.lineage.entity.Project;
import com.annapolislabs.lineage.entity.Requirement;
import com.annapolislabs.lineage.entity.User;
import com.annapolislabs.lineage.entity.UserRole;
import com.annapolislabs.lineage.exception.SearchException;
import com.annapolislabs.lineage.repository.ProjectMemberRepository;
import com.annapolislabs.lineage.repository.RequirementRepository;
import com.annapolislabs.lineage.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchControllerTest {

    @Mock
    private RequirementRepository requirementRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private AuthService authService;

    @InjectMocks
    private SearchController searchController;

    private User testUser;
    private Project testProject;
    private Requirement testRequirement;

    @BeforeEach
    void setUp() {
        testUser = new User("test@example.com", "hashedPassword", "Test User", UserRole.EDITOR);
        testUser.setId(UUID.randomUUID());

        testProject = new Project("Test Project", "Description", "TEST", testUser);
        testProject.setId(UUID.randomUUID());

        testRequirement = new Requirement(testProject, "REQ-001", "Test Requirement", "Description", testUser);
        testRequirement.setId(UUID.randomUUID());
        testRequirement.setStatus("DRAFT");
        testRequirement.setPriority("MEDIUM");
    }

    @Test
    void search_WithTextQuery_Success() {
        // Arrange
        UUID projectId = testProject.getId();
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, testUser.getId())).thenReturn(true);
        when(requirementRepository.searchByText(projectId, "test")).thenReturn(Arrays.asList(testRequirement));

        // Act
        ResponseEntity<List<RequirementResponse>> response = searchController.search(projectId, "test", null, null);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(requirementRepository).searchByText(projectId, "test");
    }

    @Test
    void search_WithStatusFilter_Success() {
        // Arrange
        UUID projectId = testProject.getId();
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, testUser.getId())).thenReturn(true);
        when(requirementRepository.findByFilters(projectId, "DRAFT", null))
                .thenReturn(Arrays.asList(testRequirement));

        // Act
        ResponseEntity<List<RequirementResponse>> response = searchController.search(projectId, null, "DRAFT", null);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(requirementRepository).findByFilters(projectId, "DRAFT", null);
    }

    @Test
    void search_WithTextAndStatusFilter_Success() {
        // Arrange
        UUID projectId = testProject.getId();
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, testUser.getId())).thenReturn(true);
        when(requirementRepository.searchByText(projectId, "test")).thenReturn(Arrays.asList(testRequirement));

        // Act
        ResponseEntity<List<RequirementResponse>> response = searchController.search(projectId, "test", "DRAFT", null);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void search_WithPriorityFilter_Success() {
        // Arrange
        UUID projectId = testProject.getId();
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, testUser.getId())).thenReturn(true);
        when(requirementRepository.findByFilters(projectId, null, "MEDIUM"))
                .thenReturn(Arrays.asList(testRequirement));

        // Act
        ResponseEntity<List<RequirementResponse>> response = searchController.search(projectId, null, null, "MEDIUM");

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void search_NoAccess_ThrowsException() {
        // Arrange
        UUID projectId = testProject.getId();
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, testUser.getId())).thenReturn(false);

        // Act & Assert
        assertThrows(SearchException.class, () -> {
            searchController.search(projectId, "test", null, null);
        });
    }

    @Test
    void search_WithAllFilters_Success() {
        // Arrange
        UUID projectId = testProject.getId();
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, testUser.getId())).thenReturn(true);
        when(requirementRepository.searchByText(projectId, "test")).thenReturn(Arrays.asList(testRequirement));

        // Act
        ResponseEntity<List<RequirementResponse>> response = searchController.search(projectId, "test", "DRAFT", "MEDIUM");

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void search_EmptyQuery_UsesFilterSearch() {
        // Arrange
        UUID projectId = testProject.getId();
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, testUser.getId())).thenReturn(true);
        when(requirementRepository.findByFilters(projectId, "DRAFT", "MEDIUM"))
                .thenReturn(Arrays.asList(testRequirement));

        // Act
        ResponseEntity<List<RequirementResponse>> response = searchController.search(projectId, "  ", "DRAFT", "MEDIUM");

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(requirementRepository).findByFilters(projectId, "DRAFT", "MEDIUM");
        verify(requirementRepository, never()).searchByText(any(), any());
    }
}
