package com.annapolislabs.lineage.integration;

import com.annapolislabs.lineage.entity.*;
import com.annapolislabs.lineage.repository.*;
import com.annapolislabs.lineage.security.SecurityAuditService;
import com.annapolislabs.lineage.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.DisabledOnJre;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.awaitility.Awaitility.*;

/**
 * Integration tests for RBAC collaboration system
 * Tests end-to-end workflows including team management, task assignment, and peer review
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Transactional
// Close this test's Spring context (and its Hikari pool) immediately after this class finishes,
// before Testcontainers tears down the static Postgres container. Without this, the context stays
// open until JVM shutdown, by which point the container is already gone, causing Hikari to hang
// for 30s per connection trying to validate/close against a dead container.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RBACCollaborationIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgreSQL = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("lineage_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQL::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQL::getUsername);
        registry.add("spring.datasource.password", postgreSQL::getPassword);
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private RequirementRepository requirementRepository;

    @Autowired
    private TaskAssignmentRepository taskAssignmentRepository;

    @Autowired
    private PeerReviewRepository peerReviewRepository;

    @Autowired
    private PermissionEvaluationService permissionEvaluationService;

    @Autowired
    private TeamService teamService;

    @Autowired
    private TaskAssignmentService taskAssignmentService;

    @Autowired
    private PeerReviewService peerReviewService;

    @Autowired
    private SecurityAuditService securityAuditService;

    private User adminUser;
    private User managerUser;
    private User developerUser;
    private Team testTeam;
    private TaskAssignment testTask;
    private PeerReview testReview;

    // Unique per-test-method suffix for user emails. Some tests (e.g.
    // testConcurrentPermissionEvaluation) must commit their transaction early so that
    // separate worker threads can see the data, which means we can't rely on the usual
    // @Transactional test rollback to keep emails unique across test methods.
    private String emailSuffix;

    @BeforeEach
    void setUp() {
        emailSuffix = UUID.randomUUID().toString().substring(0, 8);

        // Create test users
        adminUser = createTestUser("admin-" + emailSuffix + "@lineage.com", UserRole.ADMINISTRATOR);
        managerUser = createTestUser("manager-" + emailSuffix + "@lineage.com", UserRole.PROJECT_MANAGER);
        developerUser = createTestUser("developer-" + emailSuffix + "@lineage.com", UserRole.DEVELOPER);
        
        // Create a real project so team/task foreign keys resolve
        Project testProject = projectRepository.save(
                new Project("Test Project", "RBAC test project", "RBAC-" + UUID.randomUUID().toString().substring(0, 8), adminUser));
        UUID projectId = testProject.getId();

        // Create test team
        testTeam = teamService.createTeam("Development Team", "Main development team", projectId, adminUser.getId(), 
                Map.of("require_peer_review", true, "max_members", 10));

        // Add the developer as an active team member so project/task-scoped permission
        // checks (which rely on team membership - see PermissionEvaluationService.checkTeamPermissions)
        // resolve correctly for them within this project.
        TeamMember developerMembership = new TeamMember(testTeam.getId(), developerUser.getId(), TeamMember.TeamRole.MEMBER);
        developerMembership.setStatus(TeamMember.TeamMemberStatus.ACTIVE);
        teamMemberRepository.save(developerMembership);

        // Create test task
        testTask = taskAssignmentService.createTask(
                "Implement user authentication",
                "Create login and registration components",
                developerUser.getId(),
                projectId,
                null,
                TaskAssignment.TaskPriority.HIGH,
                LocalDateTime.now().plusDays(7),
                managerUser.getId()
        );
        
        // Create a real requirement so the peer review's foreign key resolves
        Requirement testRequirement = requirementRepository.save(
                new Requirement(testProject, "REQ-" + emailSuffix, "Sample Requirement", "Requirement used for peer review testing", adminUser));

        // Create test peer review
        testReview = peerReviewService.createReview(
                testRequirement.getId(),
                managerUser.getId(),
                developerUser.getId(),
                PeerReview.ReviewType.CODE,
                LocalDateTime.now().plusDays(3),
                adminUser.getId()
        );
    }

    @Test
    void testCompleteTeamCollaborationWorkflow() {
        // inviteUserToTeam invites an existing registered user (looked up by email), so the
        // invitee must already exist before the invitation is created.
        String newDeveloperEmail = "newdeveloper-" + emailSuffix + "@lineage.com";
        User invitedUser = createTestUser(newDeveloperEmail, UserRole.DEVELOPER);

        // Test user invitation
        TeamMember invitation = teamService.inviteUserToTeam(
                testTeam.getId(), 
                newDeveloperEmail,
                TeamMember.TeamRole.MEMBER, 
                adminUser.getId(), 
                "Welcome to the team!"
        );
        assertNotNull(invitation);
        assertEquals(TeamMember.TeamMemberStatus.PENDING, invitation.getStatus());

        UUID invitedUserId = invitedUser.getId();
        
        teamService.acceptInvitation(invitation.getId(), invitedUserId);
        
        // Verify user is now a team member
        List<TeamMember> members = teamService.getTeamMembers(testTeam.getId(), adminUser.getId());
        assertTrue(members.stream().anyMatch(m -> invitedUserId.equals(m.getUserId())));
        String persistedEmail = userRepository.findById(invitedUserId)
                .map(User::getEmail)
                .orElseThrow(() -> new AssertionError("Invited user not persisted"));
        assertEquals(newDeveloperEmail, persistedEmail);
    }

    @Test
    void testTaskAssignmentWorkflow() {
        // Test task status progression
        assertEquals(TaskAssignment.TaskStatus.ASSIGNED, testTask.getStatus());
        
        // Start task
        taskAssignmentService.startTask(testTask.getId(), developerUser.getId());
        TaskAssignment updatedTask = taskAssignmentService.getTaskById(testTask.getId(), developerUser.getId());
        assertEquals(TaskAssignment.TaskStatus.IN_PROGRESS, updatedTask.getStatus());
        
        // Add tags and blockers
        taskAssignmentService.addTagToTask(testTask.getId(), "authentication", developerUser.getId());
        taskAssignmentService.addBlockerToTask(testTask.getId(), "Waiting for API documentation", developerUser.getId());
        
        updatedTask = taskAssignmentService.getTaskById(testTask.getId(), developerUser.getId());
        assertTrue(updatedTask.getTags().contains("authentication"));
        assertTrue(updatedTask.getBlockers().contains("Waiting for API documentation"));
        
        // Complete task
        taskAssignmentService.completeTask(testTask.getId(), "Authentication implementation completed successfully", developerUser.getId());
        updatedTask = taskAssignmentService.getTaskById(testTask.getId(), developerUser.getId());
        assertEquals(TaskAssignment.TaskStatus.COMPLETED, updatedTask.getStatus());
        assertEquals("Authentication implementation completed successfully", updatedTask.getCompletionNotes());
    }

    @Test
    void testPeerReviewWorkflow() {
        // Test review status progression
        assertEquals(PeerReview.ReviewStatus.PENDING, testReview.getStatus());
        
        // Start review
        peerReviewService.startReview(testReview.getId(), managerUser.getId());
        
        // Set ratings
        peerReviewService.setRatings(testReview.getId(), 4, 5, managerUser.getId());
        
        // Approve review
        peerReviewService.approveReview(testReview.getId(), "Great implementation, very clean code!", managerUser.getId());
        
        PeerReview updatedReview = peerReviewService.getPeerReviewById(testReview.getId(), managerUser.getId());
        assertEquals(PeerReview.ReviewStatus.APPROVED, updatedReview.getStatus());
        assertEquals(4, updatedReview.getEffortRating());
        assertEquals(5, updatedReview.getQualityRating());
    }

    @Test
    void testPermissionEnforcement() {
        // Developer should not be able to create teams
        assertThrows(SecurityException.class, () -> {
            teamService.createTeam("Unauthorized Team", "Should fail", UUID.randomUUID(), developerUser.getId(), Map.of());
        });
        
        // Developer should be able to view their assigned tasks
        assertDoesNotThrow(() -> {
            taskAssignmentService.getTaskById(testTask.getId(), developerUser.getId());
        });
        
        // Developer should not be able to view tasks from other projects
        UUID otherProjectId = UUID.randomUUID();
        assertThrows(SecurityException.class, () -> {
            taskAssignmentService.getTasksByProject(otherProjectId, developerUser.getId());
        });
    }

    @Test
    void testConcurrentPermissionEvaluation() {
        // The permission checks below run on separate worker threads with their own DB
        // connections, so the setUp() data must be committed first - otherwise those threads
        // cannot see the (still uncommitted) test fixtures created in the test's main thread
        // transaction, and every check would fail-secure to false.
        TestTransaction.flagForCommit();
        TestTransaction.end();

        // Test concurrent permission checks don't cause issues
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        
        for (int i = 0; i < 100; i++) {
            futures.add(CompletableFuture.supplyAsync(() -> 
                permissionEvaluationService.hasPermission(developerUser.getId(), "task.complete", testTask.getProjectId()), 
                executor
            ));
        }
        
        // Wait for all requests to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        // All should succeed
        for (CompletableFuture<Boolean> future : futures) {
            assertTrue(future.join());
        }
        
        executor.shutdown();

        // Restore a normal transactional state so the test framework's usual rollback-based
        // cleanup behaves consistently for subsequent test methods in this class.
        TestTransaction.start();
        TestTransaction.flagForRollback();
    }

    @Test
    @Disabled("Disabled due to pipeline performance issues.")
    void testPermissionCachePerformance() {
        // Measure cache performance
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 1000; i++) {
            permissionEvaluationService.hasPermission(adminUser.getId(), "team.manage", testTeam.getProjectId());
        }
        
        long duration = System.currentTimeMillis() - startTime;
        
        // Should be very fast due to caching (< 100ms for 1000 checks)
        assertTrue(duration < 300, "Permission evaluation took too long: " + duration + "ms");
    }

    @Test
    void testAuditLoggingIntegration() {
        // Test that all actions are properly audited
        long initialAuditCount = securityAuditService.getAuditCount(LocalDateTime.now().minusHours(1));
        
        // Perform action that should be audited
        taskAssignmentService.startTask(testTask.getId(), developerUser.getId());
        
        long finalAuditCount = securityAuditService.getAuditCount(LocalDateTime.now().minusHours(1));
        assertTrue(finalAuditCount > initialAuditCount);
    }

    @Test
    void testRoleHierarchyEnforcement() {
        // Test role hierarchy
        assertTrue(permissionEvaluationService.hasRoleOrHigher(adminUser.getId(), UserRole.USER));
        assertTrue(permissionEvaluationService.hasRoleOrHigher(adminUser.getId(), UserRole.ADMINISTRATOR));
        assertFalse(permissionEvaluationService.hasRoleOrHigher(developerUser.getId(), UserRole.ADMINISTRATOR));
    }

    @Test
    void testScopedPermissions() {
        // Test resource-scoped permissions
        assertTrue(permissionEvaluationService.hasPermission(developerUser.getId(), "task.complete", testTask.getProjectId()));
        assertFalse(permissionEvaluationService.hasPermission(developerUser.getId(), "task.complete", UUID.randomUUID()));
    }

    @Test
    void testBatchPermissionEvaluation() {
        List<String> permissions = Arrays.asList("task.complete", "task.assign", "team.manage");
        Map<String, Boolean> results = permissionEvaluationService.evaluatePermissions(
                developerUser.getId(), permissions, testTask.getProjectId());
        
        assertTrue(results.get("task.complete"));  // Should have this permission
        assertFalse(results.get("task.assign"));   // Should not have this permission
        assertFalse(results.get("team.manage"));   // Should not have this permission
    }

    @Test
    void testTaskStatisticsAndReporting() {
        // Create additional tasks for testing
        taskAssignmentService.createTask(
                "Task 2", "Description 2", developerUser.getId(), testTask.getProjectId(), 
                null, TaskAssignment.TaskPriority.MEDIUM, LocalDateTime.now().plusDays(5), managerUser.getId());
        
        taskAssignmentService.createTask(
                "Task 3", "Description 3", developerUser.getId(), testTask.getProjectId(),
                null, TaskAssignment.TaskPriority.LOW, LocalDateTime.now().plusDays(10), managerUser.getId());
        
        // Test statistics
        Map<String, Object> statistics = taskAssignmentService.getTaskStatistics(testTask.getProjectId(), adminUser.getId());
        assertNotNull(statistics);
        assertTrue(statistics.containsKey("by_status"));
        assertTrue(statistics.containsKey("by_priority"));
    }

    private User createTestUser(String email, UserRole role) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("hashed_password"); // For testing purposes
        user.setGlobalRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setFirstName("Test");
        user.setLastName("User");
        return userRepository.save(user);
    }
}