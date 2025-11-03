package com.annapolislabs.lineage.controller;

import com.annapolislabs.lineage.dto.request.CreateRequirementRequest;
import com.annapolislabs.lineage.dto.response.RequirementResponse;
import com.annapolislabs.lineage.entity.Project;
import com.annapolislabs.lineage.entity.Requirement;
import com.annapolislabs.lineage.entity.User;
import com.annapolislabs.lineage.entity.UserRole;
import com.annapolislabs.lineage.service.RequirementService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequirementControllerTest {

    @Mock
    private RequirementService requirementService;

    @InjectMocks
    private RequirementController requirementController;

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
    void createRequirement_Success() {
        // Arrange
        UUID projectId = testProject.getId();
        CreateRequirementRequest request = new CreateRequirementRequest();
        request.setTitle("Test Requirement");
        request.setDescription("Description");
        request.setStatus("DRAFT");
        request.setPriority("MEDIUM");

        RequirementResponse requirementResponse = new RequirementResponse(testRequirement);
        when(requirementService.createRequirement(eq(projectId), any(CreateRequirementRequest.class)))
                .thenReturn(requirementResponse);

        // Act
        ResponseEntity<RequirementResponse> response = requirementController.createRequirement(projectId, request);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Test Requirement", response.getBody().getTitle());
        verify(requirementService).createRequirement(eq(projectId), any(CreateRequirementRequest.class));
    }

    @Test
    void getRequirementsByProject_Success() {
        // Arrange
        UUID projectId = testProject.getId();
        Requirement req1 = new Requirement(testProject, "REQ-001", "Requirement 1", "Desc 1", testUser);
        req1.setId(UUID.randomUUID());
        req1.setStatus("DRAFT");
        req1.setPriority("HIGH");

        Requirement req2 = new Requirement(testProject, "REQ-002", "Requirement 2", "Desc 2", testUser);
        req2.setId(UUID.randomUUID());
        req2.setStatus("APPROVED");
        req2.setPriority("MEDIUM");

        List<RequirementResponse> requirements = Arrays.asList(
                new RequirementResponse(req1),
                new RequirementResponse(req2)
        );
        when(requirementService.getRequirementsByProject(projectId)).thenReturn(requirements);

        // Act
        ResponseEntity<List<RequirementResponse>> response = requirementController.getRequirementsByProject(projectId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        verify(requirementService).getRequirementsByProject(projectId);
    }

    @Test
    void getRequirementById_Success() {
        // Arrange
        UUID requirementId = testRequirement.getId();
        RequirementResponse requirementResponse = new RequirementResponse(testRequirement);
        when(requirementService.getRequirementById(requirementId)).thenReturn(requirementResponse);

        // Act
        ResponseEntity<RequirementResponse> response = requirementController.getRequirementById(requirementId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Test Requirement", response.getBody().getTitle());
        verify(requirementService).getRequirementById(requirementId);
    }

    @Test
    void updateRequirement_Success() {
        // Arrange
        UUID requirementId = testRequirement.getId();
        testRequirement.setTitle("Updated Requirement");
        testRequirement.setDescription("Updated Description");
        testRequirement.setStatus("APPROVED");
        testRequirement.setPriority("HIGH");

        CreateRequirementRequest request = new CreateRequirementRequest();
        request.setTitle("Updated Requirement");
        request.setDescription("Updated Description");
        request.setStatus("APPROVED");
        request.setPriority("HIGH");

        RequirementResponse requirementResponse = new RequirementResponse(testRequirement);
        when(requirementService.updateRequirement(eq(requirementId), any(CreateRequirementRequest.class)))
                .thenReturn(requirementResponse);

        // Act
        ResponseEntity<RequirementResponse> response = requirementController.updateRequirement(requirementId, request);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Updated Requirement", response.getBody().getTitle());
        verify(requirementService).updateRequirement(eq(requirementId), any(CreateRequirementRequest.class));
    }

    @Test
    void deleteRequirement_Success() {
        // Arrange
        UUID requirementId = UUID.randomUUID();
        doNothing().when(requirementService).deleteRequirement(requirementId);

        // Act
        ResponseEntity<Void> response = requirementController.deleteRequirement(requirementId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(requirementService).deleteRequirement(requirementId);
    }

    @Test
    void getRequirementHistory_Success() {
        // Arrange
        UUID requirementId = UUID.randomUUID();
        List<Map<String, Object>> history = Arrays.asList(
                Map.of("id", UUID.randomUUID(), "changeType", "CREATED", "changedBy", "test@example.com"),
                Map.of("id", UUID.randomUUID(), "changeType", "UPDATED", "changedBy", "test@example.com")
        );
        when(requirementService.getRequirementHistory(requirementId)).thenReturn(history);

        // Act
        ResponseEntity<List<Map<String, Object>>> response = requirementController.getRequirementHistory(requirementId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        verify(requirementService).getRequirementHistory(requirementId);
    }
}
