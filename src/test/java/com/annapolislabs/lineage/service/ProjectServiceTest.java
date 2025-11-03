package com.annapolislabs.lineage.service;

import com.annapolislabs.lineage.dto.request.CreateProjectRequest;
import com.annapolislabs.lineage.dto.response.ProjectResponse;
import com.annapolislabs.lineage.entity.*;
import com.annapolislabs.lineage.repository.ProjectMemberRepository;
import com.annapolislabs.lineage.repository.ProjectRepository;
import com.annapolislabs.lineage.repository.RequirementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private RequirementRepository requirementRepository;

    @Mock
    private AuthService authService;

    @InjectMocks
    private ProjectService projectService;

    private User testUser;
    private Project testProject;

    @BeforeEach
    void setUp() {
        testUser = new User("test@example.com", "hashedPassword", "Test User", UserRole.ADMIN);
        testUser.setId(UUID.randomUUID());

        testProject = new Project("Test Project", "Test Description", "TEST", testUser);
        testProject.setId(UUID.randomUUID());
    }

    @Test
    void createProject_Success() {
        // Arrange
        CreateProjectRequest request = new CreateProjectRequest("Test Project", "Description", "TEST");
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(projectRepository.existsByProjectKey("TEST")).thenReturn(false);
        when(projectRepository.save(any(Project.class))).thenReturn(testProject);
        when(projectMemberRepository.save(any(ProjectMember.class))).thenReturn(new ProjectMember());

        // Act
        ProjectResponse response = projectService.createProject(request);

        // Assert
        assertNotNull(response);
        assertEquals("Test Project", response.getName());
        assertEquals("TEST", response.getProjectKey());
        verify(projectRepository).save(any(Project.class));
        verify(projectMemberRepository).save(any(ProjectMember.class));
    }

    @Test
    void createProject_DuplicateKey_ThrowsException() {
        // Arrange
        CreateProjectRequest request = new CreateProjectRequest("Test Project", "Description", "TEST");
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(projectRepository.existsByProjectKey("TEST")).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            projectService.createProject(request);
        });
        assertTrue(exception.getMessage().contains("Project key already exists"));
        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    void getAllProjects_ReturnsUserProjects() {
        // Arrange
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(projectRepository.findAllByUserId(testUser.getId()))
                .thenReturn(Arrays.asList(testProject));

        // Act
        List<ProjectResponse> projects = projectService.getAllProjects();

        // Assert
        assertNotNull(projects);
        assertEquals(1, projects.size());
        assertEquals("Test Project", projects.get(0).getName());
    }

    @Test
    void getProjectById_Success() {
        // Arrange
        when(projectRepository.findById(testProject.getId())).thenReturn(Optional.of(testProject));
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(projectMemberRepository.existsByProjectIdAndUserId(testProject.getId(), testUser.getId()))
                .thenReturn(true);

        // Act
        ProjectResponse response = projectService.getProjectById(testProject.getId());

        // Assert
        assertNotNull(response);
        assertEquals("Test Project", response.getName());
    }

    @Test
    void getProjectById_NoAccess_ThrowsException() {
        // Arrange
        when(projectRepository.findById(testProject.getId())).thenReturn(Optional.of(testProject));
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(projectMemberRepository.existsByProjectIdAndUserId(testProject.getId(), testUser.getId()))
                .thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            projectService.getProjectById(testProject.getId());
        });
        assertTrue(exception.getMessage().contains("Access denied"));
    }

    @Test
    void deleteProject_Success() {
        // Arrange
        ProjectMember member = new ProjectMember(testProject, testUser, ProjectRole.ADMIN);
        when(projectRepository.findById(testProject.getId())).thenReturn(Optional.of(testProject));
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(projectMemberRepository.findByProjectIdAndUserId(testProject.getId(), testUser.getId()))
                .thenReturn(Optional.of(member));
        when(requirementRepository.findByProjectId(testProject.getId())).thenReturn(new java.util.ArrayList<>());
        when(projectMemberRepository.findByProjectId(testProject.getId())).thenReturn(Arrays.asList(member));

        // Act
        projectService.deleteProject(testProject.getId());

        // Assert
        verify(projectRepository).delete(testProject);
    }

    @Test
    void deleteProject_NotAdmin_ThrowsException() {
        // Arrange
        ProjectMember member = new ProjectMember(testProject, testUser, ProjectRole.VIEWER);
        when(projectRepository.findById(testProject.getId())).thenReturn(Optional.of(testProject));
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(projectMemberRepository.findByProjectIdAndUserId(testProject.getId(), testUser.getId()))
                .thenReturn(Optional.of(member));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            projectService.deleteProject(testProject.getId());
        });
        assertTrue(exception.getMessage().contains("Admin access required"));
        verify(projectRepository, never()).delete(any(Project.class));
    }

    @Test
    void updateProject_Success() {
        // Arrange
        CreateProjectRequest request = new CreateProjectRequest("Updated Project", "Updated Description", "TEST");
        request.setLevelPrefixes(new java.util.HashMap<>());
        
        ProjectMember member = new ProjectMember(testProject, testUser, ProjectRole.ADMIN);
        when(projectRepository.findById(testProject.getId())).thenReturn(Optional.of(testProject));
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(projectMemberRepository.findByProjectIdAndUserId(testProject.getId(), testUser.getId()))
                .thenReturn(Optional.of(member));
        when(projectRepository.save(any(Project.class))).thenReturn(testProject);

        // Act
        ProjectResponse response = projectService.updateProject(testProject.getId(), request);

        // Assert
        assertNotNull(response);
        verify(projectRepository).save(any(Project.class));
    }

    @Test
    void updateProject_NotAdmin_ThrowsException() {
        // Arrange
        CreateProjectRequest request = new CreateProjectRequest("Updated Project", "Updated Description", "TEST");
        ProjectMember member = new ProjectMember(testProject, testUser, ProjectRole.EDITOR);
        
        when(projectRepository.findById(testProject.getId())).thenReturn(Optional.of(testProject));
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(projectMemberRepository.findByProjectIdAndUserId(testProject.getId(), testUser.getId()))
                .thenReturn(Optional.of(member));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            projectService.updateProject(testProject.getId(), request);
        });
        assertTrue(exception.getMessage().contains("Admin access required"));
        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    void updateProject_NoAccess_ThrowsException() {
        // Arrange
        CreateProjectRequest request = new CreateProjectRequest("Updated Project", "Updated Description", "TEST");
        
        when(projectRepository.findById(testProject.getId())).thenReturn(Optional.of(testProject));
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(projectMemberRepository.findByProjectIdAndUserId(testProject.getId(), testUser.getId()))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            projectService.updateProject(testProject.getId(), request);
        });
        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    void getProjectById_NotFound_ThrowsException() {
        // Arrange
        UUID randomId = UUID.randomUUID();
        when(projectRepository.findById(randomId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            projectService.getProjectById(randomId);
        });
    }

    @Test
    void deleteProject_NotFound_ThrowsException() {
        // Arrange
        UUID randomId = UUID.randomUUID();
        when(projectRepository.findById(randomId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            projectService.deleteProject(randomId);
        });
    }

    @Test
    void deleteProject_NoAccess_ThrowsException() {
        // Arrange
        when(projectRepository.findById(testProject.getId())).thenReturn(Optional.of(testProject));
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(projectMemberRepository.findByProjectIdAndUserId(testProject.getId(), testUser.getId()))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            projectService.deleteProject(testProject.getId());
        });
        verify(projectRepository, never()).delete(any(Project.class));
    }
}
