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

/**
 * Test class for DOORS-style hierarchical numbering system.
 *
 * Hierarchical numbering format:
 * - Level 1: 1, 2, 3, ...
 * - Level 2: 1.1, 1.2, 2.1, 2.2, ...
 * - Level 3: 1.1.1, 1.1.2, 1.2.1, ...
 * - Level N: parent.number
 */
@ExtendWith(MockitoExtension.class)
class HierarchicalNumberingTest {

    @Mock
    private RequirementRepository requirementRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private RequirementHistoryRepository historyRepository;

    @Mock
    private AuthService authService;

    @InjectMocks
    private RequirementService requirementService;

    private User testUser;
    private Project testProject;
    private ProjectMember testMember;

    @BeforeEach
    void setUp() {
        testUser = new User("test@example.com", "hashedPassword", "Test User", UserRole.EDITOR);
        testUser.setId(UUID.randomUUID());

        testProject = new Project("Test Project", "Description", "TEST", testUser);
        testProject.setId(UUID.randomUUID());

        testMember = new ProjectMember(testProject, testUser, ProjectRole.EDITOR);
    }

    @Test
    void createTopLevelRequirement_ShouldGenerateNumber_1() {
        // Given: A project with no existing requirements
        CreateRequirementRequest request = new CreateRequirementRequest();
        request.setTitle("First Requirement");
        request.setDescription("Top level requirement");
        request.setStatus("DRAFT");
        request.setPriority("MEDIUM");

        when(authService.getCurrentUser()).thenReturn(testUser);
        when(projectMemberRepository.findByProjectIdAndUserId(testProject.getId(), testUser.getId()))
                .thenReturn(Optional.of(testMember));
        when(projectRepository.findById(testProject.getId())).thenReturn(Optional.of(testProject));
        when(requirementRepository.findByProjectId(testProject.getId())).thenReturn(new ArrayList<>());

        Requirement savedReq = new Requirement(testProject, "1", "First Requirement", "Top level requirement", testUser);
        savedReq.setId(UUID.randomUUID());
        savedReq.setLevel(1);
        when(requirementRepository.save(any(Requirement.class))).thenReturn(savedReq);

        // When: Creating the first requirement
        RequirementResponse response = requirementService.createRequirement(testProject.getId(), request);

        // Then: It should be numbered "1" and be Level 1
        assertNotNull(response);
        assertEquals("1", response.getReqId());
        assertEquals(1, response.getLevel());
    }

    @Test
    void createSecondTopLevelRequirement_ShouldGenerateNumber_2() {
        // Given: A project with one existing Level 1 requirement
        Requirement existingReq = new Requirement(testProject, "1", "First", "Desc", testUser);
        existingReq.setId(UUID.randomUUID());
        existingReq.setLevel(1);

        CreateRequirementRequest request = new CreateRequirementRequest();
        request.setTitle("Second Requirement");
        request.setDescription("Another top level requirement");
        request.setStatus("DRAFT");
        request.setPriority("MEDIUM");

        when(authService.getCurrentUser()).thenReturn(testUser);
        when(projectMemberRepository.findByProjectIdAndUserId(testProject.getId(), testUser.getId()))
                .thenReturn(Optional.of(testMember));
        when(projectRepository.findById(testProject.getId())).thenReturn(Optional.of(testProject));
        when(requirementRepository.findByProjectId(testProject.getId()))
                .thenReturn(Arrays.asList(existingReq));

        Requirement savedReq = new Requirement(testProject, "2", "Second Requirement", "Another top level", testUser);
        savedReq.setId(UUID.randomUUID());
        savedReq.setLevel(1);
        when(requirementRepository.save(any(Requirement.class))).thenReturn(savedReq);

        // When: Creating a second top-level requirement
        RequirementResponse response = requirementService.createRequirement(testProject.getId(), request);

        // Then: It should be numbered "2" and be Level 1
        assertNotNull(response);
        assertEquals("2", response.getReqId());
        assertEquals(1, response.getLevel());
    }

    @Test
    void createChildRequirement_ShouldGenerateNumber_1_1() {
        // Given: A project with one Level 1 requirement "1"
        Requirement parentReq = new Requirement(testProject, "1", "Parent", "Parent requirement", testUser);
        parentReq.setId(UUID.randomUUID());
        parentReq.setLevel(1);

        CreateRequirementRequest request = new CreateRequirementRequest();
        request.setTitle("Child Requirement");
        request.setDescription("First child of requirement 1");
        request.setStatus("DRAFT");
        request.setPriority("MEDIUM");
        request.setParentId(parentReq.getId());

        when(authService.getCurrentUser()).thenReturn(testUser);
        when(projectMemberRepository.findByProjectIdAndUserId(testProject.getId(), testUser.getId()))
                .thenReturn(Optional.of(testMember));
        when(projectRepository.findById(testProject.getId())).thenReturn(Optional.of(testProject));
        when(requirementRepository.findByProjectId(testProject.getId()))
                .thenReturn(Arrays.asList(parentReq));
        when(requirementRepository.findById(parentReq.getId())).thenReturn(Optional.of(parentReq));

        Requirement savedReq = new Requirement(testProject, "1.1", "Child Requirement", "First child", testUser);
        savedReq.setId(UUID.randomUUID());
        savedReq.setLevel(2);
        savedReq.setParent(parentReq);
        when(requirementRepository.save(any(Requirement.class))).thenReturn(savedReq);

        // When: Creating a child under requirement "1"
        RequirementResponse response = requirementService.createRequirement(testProject.getId(), request);

        // Then: It should be numbered "1.1" and be Level 2
        assertNotNull(response);
        assertEquals("1.1", response.getReqId());
        assertEquals(2, response.getLevel());
        assertEquals(parentReq.getId().toString(), response.getParentId());
    }

    @Test
    void createSecondChildRequirement_ShouldGenerateNumber_1_2() {
        // Given: A project with requirement "1" and child "1.1"
        Requirement parentReq = new Requirement(testProject, "1", "Parent", "Parent requirement", testUser);
        parentReq.setId(UUID.randomUUID());
        parentReq.setLevel(1);

        Requirement firstChild = new Requirement(testProject, "1.1", "First Child", "First child", testUser);
        firstChild.setId(UUID.randomUUID());
        firstChild.setLevel(2);
        firstChild.setParent(parentReq);

        CreateRequirementRequest request = new CreateRequirementRequest();
        request.setTitle("Second Child");
        request.setDescription("Second child of requirement 1");
        request.setStatus("DRAFT");
        request.setPriority("MEDIUM");
        request.setParentId(parentReq.getId());

        when(authService.getCurrentUser()).thenReturn(testUser);
        when(projectMemberRepository.findByProjectIdAndUserId(testProject.getId(), testUser.getId()))
                .thenReturn(Optional.of(testMember));
        when(projectRepository.findById(testProject.getId())).thenReturn(Optional.of(testProject));
        when(requirementRepository.findByProjectId(testProject.getId()))
                .thenReturn(Arrays.asList(parentReq, firstChild));
        when(requirementRepository.findById(parentReq.getId())).thenReturn(Optional.of(parentReq));

        Requirement savedReq = new Requirement(testProject, "1.2", "Second Child", "Second child", testUser);
        savedReq.setId(UUID.randomUUID());
        savedReq.setLevel(2);
        savedReq.setParent(parentReq);
        when(requirementRepository.save(any(Requirement.class))).thenReturn(savedReq);

        // When: Creating a second child under requirement "1"
        RequirementResponse response = requirementService.createRequirement(testProject.getId(), request);

        // Then: It should be numbered "1.2" and be Level 2
        assertNotNull(response);
        assertEquals("1.2", response.getReqId());
        assertEquals(2, response.getLevel());
    }

    @Test
    void createGrandchildRequirement_ShouldGenerateNumber_1_1_1() {
        // Given: A project with requirements "1" -> "1.1"
        Requirement level1Req = new Requirement(testProject, "1", "Level 1", "Level 1", testUser);
        level1Req.setId(UUID.randomUUID());
        level1Req.setLevel(1);

        Requirement level2Req = new Requirement(testProject, "1.1", "Level 2", "Level 2", testUser);
        level2Req.setId(UUID.randomUUID());
        level2Req.setLevel(2);
        level2Req.setParent(level1Req);

        CreateRequirementRequest request = new CreateRequirementRequest();
        request.setTitle("Level 3");
        request.setDescription("Third level requirement");
        request.setStatus("DRAFT");
        request.setPriority("MEDIUM");
        request.setParentId(level2Req.getId());

        when(authService.getCurrentUser()).thenReturn(testUser);
        when(projectMemberRepository.findByProjectIdAndUserId(testProject.getId(), testUser.getId()))
                .thenReturn(Optional.of(testMember));
        when(projectRepository.findById(testProject.getId())).thenReturn(Optional.of(testProject));
        when(requirementRepository.findByProjectId(testProject.getId()))
                .thenReturn(Arrays.asList(level1Req, level2Req));
        when(requirementRepository.findById(level2Req.getId())).thenReturn(Optional.of(level2Req));

        Requirement savedReq = new Requirement(testProject, "1.1.1", "Level 3", "Third level", testUser);
        savedReq.setId(UUID.randomUUID());
        savedReq.setLevel(3);
        savedReq.setParent(level2Req);
        when(requirementRepository.save(any(Requirement.class))).thenReturn(savedReq);

        // When: Creating a grandchild under requirement "1.1"
        RequirementResponse response = requirementService.createRequirement(testProject.getId(), request);

        // Then: It should be numbered "1.1.1" and be Level 3
        assertNotNull(response);
        assertEquals("1.1.1", response.getReqId());
        assertEquals(3, response.getLevel());
    }

    @Test
    void createDeepHierarchy_ShouldGenerateCorrectNumbers() {
        // Given: A deep hierarchy 1 -> 1.1 -> 1.1.1 -> 1.1.1.1
        Requirement level1 = new Requirement(testProject, "1", "L1", "L1", testUser);
        level1.setId(UUID.randomUUID());
        level1.setLevel(1);

        Requirement level2 = new Requirement(testProject, "1.1", "L2", "L2", testUser);
        level2.setId(UUID.randomUUID());
        level2.setLevel(2);
        level2.setParent(level1);

        Requirement level3 = new Requirement(testProject, "1.1.1", "L3", "L3", testUser);
        level3.setId(UUID.randomUUID());
        level3.setLevel(3);
        level3.setParent(level2);

        CreateRequirementRequest request = new CreateRequirementRequest();
        request.setTitle("Level 4");
        request.setDescription("Fourth level requirement");
        request.setStatus("DRAFT");
        request.setPriority("MEDIUM");
        request.setParentId(level3.getId());

        when(authService.getCurrentUser()).thenReturn(testUser);
        when(projectMemberRepository.findByProjectIdAndUserId(testProject.getId(), testUser.getId()))
                .thenReturn(Optional.of(testMember));
        when(projectRepository.findById(testProject.getId())).thenReturn(Optional.of(testProject));
        when(requirementRepository.findByProjectId(testProject.getId()))
                .thenReturn(Arrays.asList(level1, level2, level3));
        when(requirementRepository.findById(level3.getId())).thenReturn(Optional.of(level3));

        Requirement savedReq = new Requirement(testProject, "1.1.1.1", "Level 4", "Fourth level", testUser);
        savedReq.setId(UUID.randomUUID());
        savedReq.setLevel(4);
        savedReq.setParent(level3);
        when(requirementRepository.save(any(Requirement.class))).thenReturn(savedReq);

        // When: Creating a 4th level requirement
        RequirementResponse response = requirementService.createRequirement(testProject.getId(), request);

        // Then: It should be numbered "1.1.1.1" and be Level 4
        assertNotNull(response);
        assertEquals("1.1.1.1", response.getReqId());
        assertEquals(4, response.getLevel());
    }

    @Test
    void createMultipleBranches_ShouldGenerateCorrectNumbers() {
        // Given: Multiple branches: 1, 1.1, 1.2, 2, 2.1
        Requirement req1 = new Requirement(testProject, "1", "Req 1", "Req 1", testUser);
        req1.setId(UUID.randomUUID());
        req1.setLevel(1);

        Requirement req1_1 = new Requirement(testProject, "1.1", "Req 1.1", "Req 1.1", testUser);
        req1_1.setId(UUID.randomUUID());
        req1_1.setLevel(2);
        req1_1.setParent(req1);

        Requirement req1_2 = new Requirement(testProject, "1.2", "Req 1.2", "Req 1.2", testUser);
        req1_2.setId(UUID.randomUUID());
        req1_2.setLevel(2);
        req1_2.setParent(req1);

        Requirement req2 = new Requirement(testProject, "2", "Req 2", "Req 2", testUser);
        req2.setId(UUID.randomUUID());
        req2.setLevel(1);

        CreateRequirementRequest request = new CreateRequirementRequest();
        request.setTitle("Req 2.1");
        request.setDescription("First child of requirement 2");
        request.setStatus("DRAFT");
        request.setPriority("MEDIUM");
        request.setParentId(req2.getId());

        when(authService.getCurrentUser()).thenReturn(testUser);
        when(projectMemberRepository.findByProjectIdAndUserId(testProject.getId(), testUser.getId()))
                .thenReturn(Optional.of(testMember));
        when(projectRepository.findById(testProject.getId())).thenReturn(Optional.of(testProject));
        when(requirementRepository.findByProjectId(testProject.getId()))
                .thenReturn(Arrays.asList(req1, req1_1, req1_2, req2));
        when(requirementRepository.findById(req2.getId())).thenReturn(Optional.of(req2));

        Requirement savedReq = new Requirement(testProject, "2.1", "Req 2.1", "First child of 2", testUser);
        savedReq.setId(UUID.randomUUID());
        savedReq.setLevel(2);
        savedReq.setParent(req2);
        when(requirementRepository.save(any(Requirement.class))).thenReturn(savedReq);

        // When: Creating first child of requirement "2"
        RequirementResponse response = requirementService.createRequirement(testProject.getId(), request);

        // Then: It should be numbered "2.1" and be Level 2
        assertNotNull(response);
        assertEquals("2.1", response.getReqId());
        assertEquals(2, response.getLevel());
    }
}
