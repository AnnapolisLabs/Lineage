package com.annapolislabs.lineage.service;

import com.annapolislabs.lineage.dto.request.CreateLinkRequest;
import com.annapolislabs.lineage.entity.*;
import com.annapolislabs.lineage.exception.AccessDeniedException;
import com.annapolislabs.lineage.exception.InvalidLinkException;
import com.annapolislabs.lineage.exception.ResourceNotFoundException;
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
class RequirementLinkServiceTest {

    @Mock
    private RequirementLinkRepository linkRepository;

    @Mock
    private RequirementRepository requirementRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private RequirementHistoryRepository historyRepository;

    @Mock
    private AuthService authService;

    @InjectMocks
    private RequirementLinkService requirementLinkService;

    private User testUser;
    private Project testProject;
    private Requirement fromRequirement;
    private Requirement toRequirement;
    private ProjectMember testMember;

    @BeforeEach
    void setUp() {
        testUser = new User("test@example.com", "password", "Test User", UserRole.USER);
        testUser.setId(UUID.randomUUID());

        testProject = new Project("PRJ", "Test Project", "Description", testUser);
        testProject.setId(UUID.randomUUID());

        fromRequirement = new Requirement(testProject, "REQ-1", "From Requirement", "Description 1", testUser);
        fromRequirement.setId(UUID.randomUUID());
        fromRequirement.setLevel(1);

        toRequirement = new Requirement(testProject, "REQ-2", "To Requirement", "Description 2", testUser);
        toRequirement.setId(UUID.randomUUID());
        toRequirement.setLevel(2);

        testMember = new ProjectMember(testProject, testUser, ProjectRole.EDITOR);
    }

    @Test
    void createLink_Success() {
        // Arrange
        CreateLinkRequest request = new CreateLinkRequest(toRequirement.getId());

        when(authService.getCurrentUser()).thenReturn(testUser);
        when(requirementRepository.findById(fromRequirement.getId())).thenReturn(Optional.of(fromRequirement));
        when(requirementRepository.findById(toRequirement.getId())).thenReturn(Optional.of(toRequirement));
        when(projectMemberRepository.findByProjectIdAndUserId(testProject.getId(), testUser.getId()))
                .thenReturn(Optional.of(testMember));
        when(linkRepository.findAllLinksForRequirement(fromRequirement.getId())).thenReturn(Collections.emptyList());

        RequirementLink savedLink = new RequirementLink(fromRequirement, toRequirement, testUser);
        savedLink.setId(UUID.randomUUID());
        when(linkRepository.save(any(RequirementLink.class))).thenReturn(savedLink);

        // Act
        Map<String, Object> result = requirementLinkService.createLink(fromRequirement.getId(), request);

        // Assert
        assertNotNull(result);
        assertEquals(savedLink.getId(), result.get("id"));
        verify(linkRepository).save(any(RequirementLink.class));
        verify(historyRepository, times(2)).save(any(RequirementHistory.class));
    }

    @Test
    void createLink_FromRequirementNotFound() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();
        CreateLinkRequest request = new CreateLinkRequest(toRequirement.getId());

        when(authService.getCurrentUser()).thenReturn(testUser);
        when(requirementRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class,
                () -> requirementLinkService.createLink(nonExistentId, request));
        verify(linkRepository, never()).save(any());
    }

    @Test
    void createLink_ToRequirementNotFound() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();
        CreateLinkRequest request = new CreateLinkRequest(nonExistentId);

        when(authService.getCurrentUser()).thenReturn(testUser);
        when(requirementRepository.findById(fromRequirement.getId())).thenReturn(Optional.of(fromRequirement));
        when(requirementRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class,
                () -> requirementLinkService.createLink(fromRequirement.getId(), request));
        verify(linkRepository, never()).save(any());
    }

    @Test
    void createLink_AccessDenied() {
        // Arrange
        CreateLinkRequest request = new CreateLinkRequest(toRequirement.getId());

        when(authService.getCurrentUser()).thenReturn(testUser);
        when(requirementRepository.findById(fromRequirement.getId())).thenReturn(Optional.of(fromRequirement));
        when(requirementRepository.findById(toRequirement.getId())).thenReturn(Optional.of(toRequirement));
        when(projectMemberRepository.findByProjectIdAndUserId(testProject.getId(), testUser.getId()))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(AccessDeniedException.class,
                () -> requirementLinkService.createLink(fromRequirement.getId(), request));
        verify(linkRepository, never()).save(any());
    }

    @Test
    void createLink_ViewerRole() {
        // Arrange
        CreateLinkRequest request = new CreateLinkRequest(toRequirement.getId());
        ProjectMember viewerMember = new ProjectMember(testProject, testUser, ProjectRole.VIEWER);

        when(authService.getCurrentUser()).thenReturn(testUser);
        when(requirementRepository.findById(fromRequirement.getId())).thenReturn(Optional.of(fromRequirement));
        when(requirementRepository.findById(toRequirement.getId())).thenReturn(Optional.of(toRequirement));
        when(projectMemberRepository.findByProjectIdAndUserId(testProject.getId(), testUser.getId()))
                .thenReturn(Optional.of(viewerMember));

        // Act & Assert
        assertThrows(InvalidLinkException.class,
                () -> requirementLinkService.createLink(fromRequirement.getId(), request));
        verify(linkRepository, never()).save(any());
    }

    @Test
    void createLink_SameLevel() {
        // Arrange
        toRequirement.setLevel(1); // Same level as fromRequirement
        CreateLinkRequest request = new CreateLinkRequest(toRequirement.getId());

        when(authService.getCurrentUser()).thenReturn(testUser);
        when(requirementRepository.findById(fromRequirement.getId())).thenReturn(Optional.of(fromRequirement));
        when(requirementRepository.findById(toRequirement.getId())).thenReturn(Optional.of(toRequirement));
        when(projectMemberRepository.findByProjectIdAndUserId(testProject.getId(), testUser.getId()))
                .thenReturn(Optional.of(testMember));

        // Act & Assert
        assertThrows(InvalidLinkException.class,
                () -> requirementLinkService.createLink(fromRequirement.getId(), request));
        verify(linkRepository, never()).save(any());
    }

    @Test
    void createLink_LinkAlreadyExists() {
        // Arrange
        CreateLinkRequest request = new CreateLinkRequest(toRequirement.getId());
        RequirementLink existingLink = new RequirementLink(fromRequirement, toRequirement, testUser);

        when(authService.getCurrentUser()).thenReturn(testUser);
        when(requirementRepository.findById(fromRequirement.getId())).thenReturn(Optional.of(fromRequirement));
        when(requirementRepository.findById(toRequirement.getId())).thenReturn(Optional.of(toRequirement));
        when(projectMemberRepository.findByProjectIdAndUserId(testProject.getId(), testUser.getId()))
                .thenReturn(Optional.of(testMember));
        when(linkRepository.findAllLinksForRequirement(fromRequirement.getId()))
                .thenReturn(Collections.singletonList(existingLink));

        // Act & Assert
        assertThrows(InvalidLinkException.class,
                () -> requirementLinkService.createLink(fromRequirement.getId(), request));
        verify(linkRepository, never()).save(any());
    }

    @Test
    void getAllLinksForRequirement_Success() {
        // Arrange
        RequirementLink link = new RequirementLink(fromRequirement, toRequirement, testUser);
        link.setId(UUID.randomUUID());

        when(requirementRepository.findById(fromRequirement.getId())).thenReturn(Optional.of(fromRequirement));
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(projectMemberRepository.existsByProjectIdAndUserId(testProject.getId(), testUser.getId())).thenReturn(true);
        when(linkRepository.findAllLinksForRequirement(fromRequirement.getId()))
                .thenReturn(Collections.singletonList(link));

        // Act
        List<Map<String, Object>> result = requirementLinkService.getAllLinksForRequirement(fromRequirement.getId());

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(link.getId(), result.get(0).get("id"));
        assertEquals("outgoing", result.get(0).get("direction")); // toRequirement is level 2, higher than fromRequirement level 1
    }

    @Test
    void getAllLinksForRequirement_IncomingDirection() {
        // Arrange
        RequirementLink link = new RequirementLink(toRequirement, fromRequirement, testUser);
        link.setId(UUID.randomUUID());

        when(requirementRepository.findById(toRequirement.getId())).thenReturn(Optional.of(toRequirement));
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(projectMemberRepository.existsByProjectIdAndUserId(testProject.getId(), testUser.getId())).thenReturn(true);
        when(linkRepository.findAllLinksForRequirement(toRequirement.getId()))
                .thenReturn(Collections.singletonList(link));

        // Act
        List<Map<String, Object>> result = requirementLinkService.getAllLinksForRequirement(toRequirement.getId());

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("incoming", result.get(0).get("direction")); // fromRequirement is level 1, lower than toRequirement level 2
    }

    @Test
    void getAllLinksForRequirement_RequirementNotFound() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();

        when(requirementRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> requirementLinkService.getAllLinksForRequirement(nonExistentId));
    }

    @Test
    void getAllLinksForRequirement_AccessDenied() {
        // Arrange
        when(requirementRepository.findById(fromRequirement.getId())).thenReturn(Optional.of(fromRequirement));
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(projectMemberRepository.existsByProjectIdAndUserId(testProject.getId(), testUser.getId())).thenReturn(false);

        // Act & Assert
        assertThrows(InvalidLinkException.class,
                () -> requirementLinkService.getAllLinksForRequirement(fromRequirement.getId()));
    }

    @Test
    void deleteLink_Success() {
        // Arrange
        RequirementLink link = new RequirementLink(fromRequirement, toRequirement, testUser);
        UUID linkId = UUID.randomUUID();
        link.setId(linkId);

        when(linkRepository.findById(linkId)).thenReturn(Optional.of(link));
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(projectMemberRepository.findByProjectIdAndUserId(testProject.getId(), testUser.getId()))
                .thenReturn(Optional.of(testMember));

        // Act
        requirementLinkService.deleteLink(linkId);

        // Assert
        verify(linkRepository).delete(link);
        verify(historyRepository, times(2)).save(any(RequirementHistory.class));
    }

    @Test
    void deleteLink_LinkNotFound() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();

        when(linkRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> requirementLinkService.deleteLink(nonExistentId));
        verify(linkRepository, never()).delete(any());
    }

    @Test
    void deleteLink_AccessDenied() {
        // Arrange
        RequirementLink link = new RequirementLink(fromRequirement, toRequirement, testUser);
        UUID linkId = UUID.randomUUID();
        link.setId(linkId);

        when(linkRepository.findById(linkId)).thenReturn(Optional.of(link));
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(projectMemberRepository.findByProjectIdAndUserId(testProject.getId(), testUser.getId()))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(AccessDeniedException.class,
                () -> requirementLinkService.deleteLink(linkId));
        verify(linkRepository, never()).delete(any());
    }

    @Test
    void deleteLink_ViewerRole() {
        // Arrange
        RequirementLink link = new RequirementLink(fromRequirement, toRequirement, testUser);
        UUID linkId = UUID.randomUUID();
        link.setId(linkId);
        ProjectMember viewerMember = new ProjectMember(testProject, testUser, ProjectRole.VIEWER);

        when(linkRepository.findById(linkId)).thenReturn(Optional.of(link));
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(projectMemberRepository.findByProjectIdAndUserId(testProject.getId(), testUser.getId()))
                .thenReturn(Optional.of(viewerMember));

        // Act & Assert
        assertThrows(InvalidLinkException.class,
                () -> requirementLinkService.deleteLink(linkId));
        verify(linkRepository, never()).delete(any());
    }
}
