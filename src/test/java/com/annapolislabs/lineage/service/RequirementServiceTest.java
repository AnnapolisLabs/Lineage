package com.annapolislabs.lineage.service;

import com.annapolislabs.lineage.dto.request.CreateRequirementRequest;
import com.annapolislabs.lineage.dto.response.RequirementResponse;
import com.annapolislabs.lineage.entity.*;
import com.annapolislabs.lineage.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequirementServiceTest {

    @Mock
    private RequirementRepository requirementRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private RequirementHistoryRepository historyRepository;

    @Mock
    private RequirementLinkRepository linkRepository;

    @Mock
    private AuthService authService;

    @InjectMocks
    private RequirementService requirementService;

    private User testUser;
    private Project testProject;
    private Requirement testRequirement;
    private ProjectMember testMember;

    @BeforeEach
    void setUp() {
        testUser = new User("test@example.com", "hashedPassword", "Test User", UserRole.EDITOR);
        testUser.setId(UUID.randomUUID());

        testProject = new Project("Test Project", "Description", "TEST", testUser);
        testProject.setId(UUID.randomUUID());

        testRequirement = new Requirement(testProject, "TEST-001", "Test Requirement", "Description", testUser);
        testRequirement.setId(UUID.randomUUID());

        testMember = new ProjectMember(testProject, testUser, ProjectRole.EDITOR);
    }

    @Test
    void createRequirement_Success() {
        // Arrange
        CreateRequirementRequest request = new CreateRequirementRequest();
        request.setTitle("New Requirement");
        request.setDescription("Description");
        request.setStatus("DRAFT");
        request.setPriority("MEDIUM");

        when(authService.getCurrentUser()).thenReturn(testUser);
        when(projectMemberRepository.findByProjectIdAndUserId(testProject.getId(), testUser.getId()))
                .thenReturn(Optional.of(testMember));
        when(projectRepository.findById(testProject.getId())).thenReturn(Optional.of(testProject));
        when(requirementRepository.findByProjectId(testProject.getId())).thenReturn(new ArrayList<>());
        when(requirementRepository.save(any(Requirement.class))).thenReturn(testRequirement);
        when(historyRepository.save(any(RequirementHistory.class))).thenReturn(new RequirementHistory());
        when(linkRepository.findAllLinksForRequirement(any(UUID.class))).thenReturn(new ArrayList<>());

        // Act
        RequirementResponse response = requirementService.createRequirement(testProject.getId(), request);

        // Assert
        assertNotNull(response);
        verify(requirementRepository).save(any(Requirement.class));
        verify(historyRepository).save(any(RequirementHistory.class));
    }

    @Test
    void createRequirement_ViewerRole_ThrowsException() {
        // Arrange
        CreateRequirementRequest request = new CreateRequirementRequest();
        request.setTitle("New Requirement");

        ProjectMember viewerMember = new ProjectMember(testProject, testUser, ProjectRole.VIEWER);
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(projectMemberRepository.findByProjectIdAndUserId(testProject.getId(), testUser.getId()))
                .thenReturn(Optional.of(viewerMember));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            requirementService.createRequirement(testProject.getId(), request);
        });
        assertTrue(exception.getMessage().contains("Editor access required"));
        verify(requirementRepository, never()).save(any(Requirement.class));
    }

    @Test
    void getRequirementsByProject_Success() {
        // Arrange
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(projectMemberRepository.existsByProjectIdAndUserId(testProject.getId(), testUser.getId()))
                .thenReturn(true);
        when(requirementRepository.findByProjectIdAndDeletedAtIsNull(testProject.getId()))
                .thenReturn(Arrays.asList(testRequirement));
        when(linkRepository.findAllLinksForRequirement(testRequirement.getId()))
                .thenReturn(new ArrayList<>());

        // Act
        List<RequirementResponse> requirements = requirementService.getRequirementsByProject(testProject.getId());

        // Assert
        assertNotNull(requirements);
        assertEquals(1, requirements.size());
        assertEquals("TEST-001", requirements.get(0).getReqId());
    }

    @Test
    void updateRequirement_Success() {
        // Arrange
        CreateRequirementRequest request = new CreateRequirementRequest();
        request.setTitle("Updated Title");
        request.setDescription("Updated Description");
        request.setStatus("APPROVED");
        request.setPriority("HIGH");

        when(requirementRepository.findById(testRequirement.getId())).thenReturn(Optional.of(testRequirement));
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(projectMemberRepository.findByProjectIdAndUserId(testProject.getId(), testUser.getId()))
                .thenReturn(Optional.of(testMember));
        when(requirementRepository.save(any(Requirement.class))).thenReturn(testRequirement);
        when(historyRepository.save(any(RequirementHistory.class))).thenReturn(new RequirementHistory());
        when(linkRepository.findAllLinksForRequirement(any(UUID.class))).thenReturn(new ArrayList<>());

        // Act
        RequirementResponse response = requirementService.updateRequirement(testRequirement.getId(), request);

        // Assert
        assertNotNull(response);
        verify(requirementRepository).save(any(Requirement.class));
        verify(historyRepository).save(any(RequirementHistory.class));
    }

    @Test
    void deleteRequirement_Success() {
        // Arrange
        when(requirementRepository.findById(testRequirement.getId())).thenReturn(Optional.of(testRequirement));
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(projectMemberRepository.findByProjectIdAndUserId(testProject.getId(), testUser.getId()))
                .thenReturn(Optional.of(testMember));
        when(requirementRepository.save(any(Requirement.class))).thenReturn(testRequirement);
        when(historyRepository.save(any(RequirementHistory.class))).thenReturn(new RequirementHistory());

        // Act
        requirementService.deleteRequirement(testRequirement.getId());

        // Assert
        verify(requirementRepository).save(any(Requirement.class));
        verify(historyRepository).save(any(RequirementHistory.class));
    }

    @Test
    void getRequirementHistory_Success() {
        // Arrange
        RequirementHistory history = new RequirementHistory(
                testRequirement, testUser, ChangeType.CREATED, null, new HashMap<>()
        );

        when(requirementRepository.findById(testRequirement.getId())).thenReturn(Optional.of(testRequirement));
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(projectMemberRepository.existsByProjectIdAndUserId(testProject.getId(), testUser.getId()))
                .thenReturn(true);
        when(historyRepository.findByRequirementIdOrderByChangedAtDesc(testRequirement.getId()))
                .thenReturn(Arrays.asList(history));

        // Act
        List<Map<String, Object>> historyList = requirementService.getRequirementHistory(testRequirement.getId());

        // Assert
        assertNotNull(historyList);
        assertEquals(1, historyList.size());
    }
}
