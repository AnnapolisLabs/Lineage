package com.annapolislabs.lineage.controller;

import com.annapolislabs.lineage.dto.request.CreateProjectRequest;
import com.annapolislabs.lineage.dto.request.ImportProjectMetadata;
import com.annapolislabs.lineage.dto.request.ImportProjectRequest;
import com.annapolislabs.lineage.dto.request.ImportRequirementRequest;
import com.annapolislabs.lineage.dto.response.ProjectResponse;
import com.annapolislabs.lineage.entity.Project;
import com.annapolislabs.lineage.entity.User;
import com.annapolislabs.lineage.entity.UserRole;
import com.annapolislabs.lineage.service.ProjectImportService;
import com.annapolislabs.lineage.service.ProjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectControllerTest {

    @Mock
    private ProjectService projectService;

    @Mock
    private ProjectImportService projectImportService;

    @InjectMocks
    private ProjectController projectController;

    private User testUser;
    private Project testProject;

    @BeforeEach
    void setUp() {
        testUser = new User("test@example.com", "hashedPassword", "Test User", UserRole.ADMIN);
        testUser.setId(UUID.randomUUID());
        testProject = new Project("Test Project", "Description", "TEST", testUser);
        testProject.setId(UUID.randomUUID());
    }

    @Test
    void importProjectFromJson_Success() {
        ImportProjectRequest request = buildImportRequest();
        ProjectImportService.ImportResult result = new ProjectImportService.ImportResult(
                new ProjectResponse(testProject),
                List.of()
        );
        when(projectImportService.importProject(any(ImportProjectRequest.class))).thenReturn(result);

        ResponseEntity<Map<String, Object>> response = projectController.importProject(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(projectImportService).importProject(any(ImportProjectRequest.class));
    }

    @Test
    void createProject_Success() {
        // Arrange
        CreateProjectRequest request = new CreateProjectRequest("Test Project", "Description", "TEST");
        ProjectResponse projectResponse = new ProjectResponse(testProject);
        when(projectService.createProject(any(CreateProjectRequest.class))).thenReturn(projectResponse);

        // Act
        ResponseEntity<ProjectResponse> response = projectController.createProject(request);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Test Project", response.getBody().getName());
        verify(projectService).createProject(any(CreateProjectRequest.class));
    }

    @Test
    void getAllProjects_Success() {
        // Arrange
        Project project1 = new Project("Project 1", "Desc 1", "PROJ1", testUser);
        project1.setId(UUID.randomUUID());
        Project project2 = new Project("Project 2", "Desc 2", "PROJ2", testUser);
        project2.setId(UUID.randomUUID());

        List<ProjectResponse> projects = Arrays.asList(
                new ProjectResponse(project1),
                new ProjectResponse(project2)
        );
        when(projectService.getAllProjects()).thenReturn(projects);

        // Act
        ResponseEntity<List<ProjectResponse>> response = projectController.getAllProjects();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        verify(projectService).getAllProjects();
    }

    @Test
    void getProjectById_Success() {
        // Arrange
        UUID projectId = testProject.getId();
        ProjectResponse projectResponse = new ProjectResponse(testProject);
        when(projectService.getProjectById(projectId)).thenReturn(projectResponse);

        // Act
        ResponseEntity<ProjectResponse> response = projectController.getProjectById(projectId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Test Project", response.getBody().getName());
        verify(projectService).getProjectById(projectId);
    }

    @Test
    void updateProject_Success() {
        // Arrange
        UUID projectId = testProject.getId();
        testProject.setName("Updated Project");
        testProject.setDescription("Updated Description");
        CreateProjectRequest request = new CreateProjectRequest("Updated Project", "Updated Description", "TEST");
        ProjectResponse projectResponse = new ProjectResponse(testProject);
        when(projectService.updateProject(eq(projectId), any(CreateProjectRequest.class))).thenReturn(projectResponse);

        // Act
        ResponseEntity<ProjectResponse> response = projectController.updateProject(projectId, request);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Updated Project", response.getBody().getName());
        verify(projectService).updateProject(eq(projectId), any(CreateProjectRequest.class));
    }

    @Test
    void deleteProject_Success() {
        UUID projectId = UUID.randomUUID();
        doNothing().when(projectService).deleteProject(projectId);

        ResponseEntity<Void> response = projectController.deleteProject(projectId);

        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(projectService).deleteProject(projectId);
    }

    private ImportProjectRequest buildImportRequest() {
        ImportProjectMetadata metadata = new ImportProjectMetadata();
        metadata.setName("Imported");
        metadata.setKey("IMP");
        metadata.setDescription("Imported project");

        ImportRequirementRequest requirement = new ImportRequirementRequest();
        requirement.setReqId("REQ-001");
        requirement.setTitle("Imported Requirement");

        ImportProjectRequest request = new ImportProjectRequest();
        request.setProject(metadata);
        request.setRequirements(List.of(requirement));
        return request;
    }
}
