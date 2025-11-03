package com.annapolislabs.lineage.service;

import com.annapolislabs.lineage.entity.Project;
import com.annapolislabs.lineage.entity.Requirement;
import com.annapolislabs.lineage.entity.User;
import com.annapolislabs.lineage.entity.UserRole;
import com.annapolislabs.lineage.exception.ExportException;
import com.annapolislabs.lineage.exception.ResourceNotFoundException;
import com.annapolislabs.lineage.repository.ProjectMemberRepository;
import com.annapolislabs.lineage.repository.ProjectRepository;
import com.annapolislabs.lineage.repository.RequirementRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    @Mock
    private RequirementRepository requirementRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private AuthService authService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ObjectWriter objectWriter;

    @InjectMocks
    private ExportService exportService;

    private User testUser;
    private Project testProject;
    private UUID projectId;
    private Requirement requirement1;
    private Requirement requirement2;

    @BeforeEach
    void setUp() {
        testUser = new User("test@example.com", "password", "Test User", UserRole.VIEWER);
        testUser.setId(UUID.randomUUID());

        projectId = UUID.randomUUID();
        testProject = new Project("PRJ", "Test Project", "Test Description", testUser);
        testProject.setId(projectId);

        requirement1 = new Requirement(testProject, "REQ-1", "Requirement 1", "Description 1", testUser);
        requirement1.setId(UUID.randomUUID());
        requirement1.setStatus("New");
        requirement1.setPriority("High");
        requirement1.setCreatedAt(LocalDateTime.now());

        requirement2 = new Requirement(testProject, "REQ-2", "Requirement 2", "Description 2", testUser);
        requirement2.setId(UUID.randomUUID());
        requirement2.setStatus("In Progress");
        requirement2.setPriority("Medium");
        requirement2.setCreatedAt(LocalDateTime.now());
        requirement2.setParent(requirement1);
    }

    @Test
    void exportToCsv_Success() {
        // Arrange
        List<Requirement> requirements = Arrays.asList(requirement1, requirement2);

        when(authService.getCurrentUser()).thenReturn(testUser);
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, testUser.getId())).thenReturn(true);
        when(requirementRepository.findByProjectId(projectId)).thenReturn(requirements);

        // Act
        String result = exportService.exportToCsv(projectId);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("REQ_ID,Title,Description,Status,Priority,Parent,Created By,Created At"));
        assertTrue(result.contains("REQ-1"));
        assertTrue(result.contains("REQ-2"));
        assertTrue(result.contains("Requirement 1"));
        assertTrue(result.contains("Requirement 2"));
    }

    @Test
    void exportToCsv_AccessDenied() {
        // Arrange
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, testUser.getId())).thenReturn(false);

        // Act & Assert
        assertThrows(ExportException.class, () -> exportService.exportToCsv(projectId));
        verify(requirementRepository, never()).findByProjectId(any());
    }

    @Test
    void exportToCsv_WithSpecialCharacters() {
        // Arrange
        requirement1.setDescription("Description with, comma");
        requirement2.setDescription("Description with \"quotes\"");

        List<Requirement> requirements = Arrays.asList(requirement1, requirement2);

        when(authService.getCurrentUser()).thenReturn(testUser);
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, testUser.getId())).thenReturn(true);
        when(requirementRepository.findByProjectId(projectId)).thenReturn(requirements);

        // Act
        String result = exportService.exportToCsv(projectId);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("\"Description with, comma\""));
        assertTrue(result.contains("\"Description with \"\"quotes\"\"\""));
    }

    @Test
    void exportToCsv_NullValues() {
        // Arrange
        requirement1.setDescription(null);
        requirement1.setParent(null);

        List<Requirement> requirements = Collections.singletonList(requirement1);

        when(authService.getCurrentUser()).thenReturn(testUser);
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, testUser.getId())).thenReturn(true);
        when(requirementRepository.findByProjectId(projectId)).thenReturn(requirements);

        // Act
        String result = exportService.exportToCsv(projectId);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("REQ-1"));
    }

    @Test
    void exportToJson_Success() throws Exception {
        // Arrange
        List<Requirement> requirements = Arrays.asList(requirement1, requirement2);
        String expectedJson = "{\"project\": {}, \"requirements\": []}";

        when(authService.getCurrentUser()).thenReturn(testUser);
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, testUser.getId())).thenReturn(true);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
        when(requirementRepository.findByProjectId(projectId)).thenReturn(requirements);
        when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(objectWriter);
        when(objectWriter.writeValueAsString(any())).thenReturn(expectedJson);

        // Act
        String result = exportService.exportToJson(projectId);

        // Assert
        assertNotNull(result);
        assertEquals(expectedJson, result);
        verify(objectWriter).writeValueAsString(any(Map.class));
    }

    @Test
    void exportToJson_ProjectNotFound() {
        // Arrange
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, testUser.getId())).thenReturn(true);
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> exportService.exportToJson(projectId));
    }

    @Test
    void exportToJson_SerializationError() throws Exception {
        // Arrange
        List<Requirement> requirements = Collections.singletonList(requirement1);

        when(authService.getCurrentUser()).thenReturn(testUser);
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, testUser.getId())).thenReturn(true);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
        when(requirementRepository.findByProjectId(projectId)).thenReturn(requirements);
        when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(objectWriter);
        when(objectWriter.writeValueAsString(any())).thenThrow(new RuntimeException("Serialization error"));

        // Act & Assert
        assertThrows(ExportException.class, () -> exportService.exportToJson(projectId));
    }

    @Test
    void exportToJson_AccessDenied() {
        // Arrange
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, testUser.getId())).thenReturn(false);

        // Act & Assert
        assertThrows(ExportException.class, () -> exportService.exportToJson(projectId));
    }

    @Test
    void exportToMarkdown_Success() {
        // Arrange
        List<Requirement> topLevelRequirements = Collections.singletonList(requirement1);
        List<Requirement> childRequirements = Collections.singletonList(requirement2);

        when(authService.getCurrentUser()).thenReturn(testUser);
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, testUser.getId())).thenReturn(true);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
        when(requirementRepository.findByProjectIdAndParentIsNullAndDeletedAtIsNull(projectId))
                .thenReturn(topLevelRequirements);
        when(requirementRepository.findByParentIdAndDeletedAtIsNull(requirement1.getId()))
                .thenReturn(childRequirements);
        when(requirementRepository.findByParentIdAndDeletedAtIsNull(requirement2.getId()))
                .thenReturn(Collections.emptyList());

        // Act
        String result = exportService.exportToMarkdown(projectId);

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty(), "Result should not be empty");
        assertTrue(result.contains("Test Project"), "Should contain project name: " + result);
        assertTrue(result.contains("REQ-1"), "Should contain REQ-1: " + result);
    }

    @Test
    void exportToMarkdown_ProjectWithoutDescription() {
        // Arrange
        Project projectWithoutDesc = new Project("PRJ", "Test Project", null, testUser);
        projectWithoutDesc.setId(projectId);

        List<Requirement> topLevelRequirements = Collections.singletonList(requirement1);

        when(authService.getCurrentUser()).thenReturn(testUser);
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, testUser.getId())).thenReturn(true);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(projectWithoutDesc));
        when(requirementRepository.findByProjectIdAndParentIsNullAndDeletedAtIsNull(projectId))
                .thenReturn(topLevelRequirements);
        when(requirementRepository.findByParentIdAndDeletedAtIsNull(requirement1.getId()))
                .thenReturn(Collections.emptyList());

        // Act
        String result = exportService.exportToMarkdown(projectId);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("Test Project"), "Should contain project name");
        assertFalse(result.contains("Test Description"), "Should not contain description");
    }

    @Test
    void exportToMarkdown_RequirementWithoutDescription() {
        // Arrange
        requirement1.setDescription(null);
        List<Requirement> topLevelRequirements = Collections.singletonList(requirement1);

        when(authService.getCurrentUser()).thenReturn(testUser);
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, testUser.getId())).thenReturn(true);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
        when(requirementRepository.findByProjectIdAndParentIsNullAndDeletedAtIsNull(projectId))
                .thenReturn(topLevelRequirements);
        when(requirementRepository.findByParentIdAndDeletedAtIsNull(requirement1.getId()))
                .thenReturn(Collections.emptyList());

        // Act
        String result = exportService.exportToMarkdown(projectId);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("## REQ-1: Requirement 1"));
    }

    @Test
    void exportToMarkdown_ProjectNotFound() {
        // Arrange
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, testUser.getId())).thenReturn(true);
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> exportService.exportToMarkdown(projectId));
    }

    @Test
    void exportToMarkdown_AccessDenied() {
        // Arrange
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, testUser.getId())).thenReturn(false);

        // Act & Assert
        assertThrows(ExportException.class, () -> exportService.exportToMarkdown(projectId));
    }
}
