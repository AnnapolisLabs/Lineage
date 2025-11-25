package com.annapolislabs.lineage.service;

import com.annapolislabs.lineage.dto.request.CreateRequirementRequest;
import com.annapolislabs.lineage.entity.*;
import com.annapolislabs.lineage.repository.*;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
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
 * Test class for PostgreSQL VARCHAR constraint fixes in requirement ID generation and validation.
 * 
 * This class validates that the fixes for the PostgreSQL VARCHAR(255) error work correctly:
 * 1. Database constraints are properly enforced through application-level validation
 * 2. Application-level validation prevents oversized requirement IDs
 * 3. Entity validation annotations (@Size, @NotBlank) work correctly
 * 4. The overall solution prevents PostgreSQL constraint violations
 */
@ExtendWith(MockitoExtension.class)
class RequirementIdValidationTest {

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
    private ProjectMember testMember;
    private Validator validator;

    @BeforeEach
    void setUp() {
        testUser = new User("test@example.com", "hashedPassword", "Test User", UserRole.DEVELOPER);
        testUser.setId(UUID.randomUUID());

        testProject = new Project("Test Project", "Description", "TEST", testUser);
        testProject.setId(UUID.randomUUID());

        testMember = new ProjectMember(testProject, testUser, ProjectRole.EDITOR);
        
        // Initialize the Bean Validation validator
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // ====================================================================
    // NORMAL REQUIREMENT ID GENERATION TESTS
    // ====================================================================

    @Test
    void generateReqId_WithDefaultPrefix_ShouldSucceed() {
        // Given: A project with default prefix configuration
        Map<String, String> levelPrefixes = new HashMap<>();
        levelPrefixes.put("1", "REQ-L1");
        testProject.setLevelPrefixes(levelPrefixes);

        List<Requirement> existingRequirements = new ArrayList<>();

        // When: Generating requirement ID for level 1
        String reqId = callGenerateReqId(testProject, existingRequirements, 1);

        // Then: Should generate valid requirement ID within constraints
        assertEquals("REQ-L1-001", reqId);
        assertTrue(reqId.length() <= 200, "Generated requirement ID should not exceed 200 characters");
        assertFalse(reqId.isBlank(), "Generated requirement ID should not be blank");
    }

    @Test
    void generateReqId_WithShortCustomPrefix_ShouldSucceed() {
        // Given: A project with a short custom prefix
        Map<String, String> levelPrefixes = new HashMap<>();
        levelPrefixes.put("1", "CR");
        levelPrefixes.put("2", "SYS");
        testProject.setLevelPrefixes(levelPrefixes);

        List<Requirement> existingRequirements = new ArrayList<>();

        // When: Generating requirement ID for level 1
        String reqId = callGenerateReqId(testProject, existingRequirements, 1);

        // Then: Should generate valid requirement ID
        assertEquals("CR-001", reqId);
        assertTrue(reqId.length() < 50, "Generated requirement ID should be reasonably short");
    }

    @Test
    void generateReqId_WithMediumCustomPrefix_ShouldSucceed() {
        // Given: A project with a medium-length custom prefix
        String mediumPrefix = "REQUIREMENT_SPECIFICATION_SYSTEM";
        Map<String, String> levelPrefixes = new HashMap<>();
        levelPrefixes.put("1", mediumPrefix);
        testProject.setLevelPrefixes(levelPrefixes);

        List<Requirement> existingRequirements = new ArrayList<>();

        // When: Generating requirement ID for level 1
        String reqId = callGenerateReqId(testProject, existingRequirements, 1);

        // Then: Should generate valid requirement ID
        String expectedReqId = mediumPrefix + "-001";
        assertEquals(expectedReqId, reqId);
        assertTrue(reqId.length() <= 200, "Generated requirement ID should not exceed 200 characters");
        assertEquals(expectedReqId.length(), reqId.length());
    }

    @Test
    void generateReqId_WithMultipleSequentialRequirements_ShouldSucceed() {
        // Given: A project with existing requirements at the same level
        Map<String, String> levelPrefixes = new HashMap<>();
        levelPrefixes.put("1", "TEST");
        testProject.setLevelPrefixes(levelPrefixes);

        Requirement req1 = new Requirement(testProject, "TEST-001", "Test 1", "Desc 1", testUser);
        req1.setLevel(1);
        
        Requirement req2 = new Requirement(testProject, "TEST-002", "Test 2", "Desc 2", testUser);
        req2.setLevel(1);
        
        Requirement req3 = new Requirement(testProject, "TEST-003", "Test 3", "Desc 3", testUser);
        req3.setLevel(1);

        List<Requirement> existingRequirements = Arrays.asList(req1, req2, req3);

        // When: Generating requirement ID for the next level 1 requirement
        String reqId = callGenerateReqId(testProject, existingRequirements, 1);

        // Then: Should generate the next sequential number
        assertEquals("TEST-004", reqId);
    }

    // ====================================================================
    // EDGE CASES FOR CUSTOM PREFIX LENGTHS
    // ====================================================================

    @Test
    void generateReqId_WithPrefixAtBoundary_190Chars_ShouldSucceed() {
        // Given: A project with a prefix exactly at the 190-character boundary
        String boundaryPrefix = "A".repeat(190); // 190 characters
        Map<String, String> levelPrefixes = new HashMap<>();
        levelPrefixes.put("1", boundaryPrefix);
        testProject.setLevelPrefixes(levelPrefixes);

        List<Requirement> existingRequirements = new ArrayList<>();

        // When: Generating requirement ID for level 1
        String reqId = callGenerateReqId(testProject, existingRequirements, 1);

        // Then: Should generate valid requirement ID
        String expectedReqId = boundaryPrefix + "-001";
        assertEquals(expectedReqId, reqId);
        assertTrue(reqId.length() <= 200, "Generated requirement ID should not exceed 200 characters");
        assertEquals(expectedReqId.length(), reqId.length(), "Generated requirement ID should match expected length");
        assertEquals(194, reqId.length(), "Generated requirement ID should be 190 + '-' + '001' = 194 characters");
    }

    @Test
    void generateReqId_WithPrefixOneCharOverBoundary_191Chars_ShouldThrowException() {
        // Given: A project with a prefix that exceeds the 190-character limit
        String oversizedPrefix = "A".repeat(191); // 191 characters
        Map<String, String> levelPrefixes = new HashMap<>();
        levelPrefixes.put("1", oversizedPrefix);
        testProject.setLevelPrefixes(levelPrefixes);

        List<Requirement> existingRequirements = new ArrayList<>();

        // When & Then: Should throw IllegalArgumentException
        Throwable exception = assertThrows(Throwable.class, () -> {
            callGenerateReqId(testProject, existingRequirements, 1);
        });

        assertTrue(exception instanceof IllegalArgumentException, "Should throw IllegalArgumentException");
        IllegalArgumentException iae = (IllegalArgumentException) exception;
        assertTrue(iae.getMessage().contains("exceeds maximum length of 190 characters"));
        assertTrue(iae.getMessage().contains("Current length: 191"));
    }

    @Test
    void generateReqId_WithPrefixSignificantlyOverBoundary_250Chars_ShouldThrowException() {
        // Given: A project with a significantly oversized prefix
        String veryLargePrefix = "A".repeat(250); // 250 characters
        Map<String, String> levelPrefixes = new HashMap<>();
        levelPrefixes.put("1", veryLargePrefix);
        testProject.setLevelPrefixes(levelPrefixes);

        List<Requirement> existingRequirements = new ArrayList<>();

        // When & Then: Should throw IllegalArgumentException
        Throwable exception = assertThrows(Throwable.class, () -> {
            callGenerateReqId(testProject, existingRequirements, 1);
        });

        assertTrue(exception instanceof IllegalArgumentException, "Should throw IllegalArgumentException");
        IllegalArgumentException iae = (IllegalArgumentException) exception;
        assertTrue(iae.getMessage().contains("exceeds maximum length of 190 characters"));
        assertTrue(iae.getMessage().contains("Current length: 250"));
    }

    @Test
    void generateReqId_WithLongPrefix_ScenarioThatCouldCauseDBError_ShouldBePrevented() {
        // Given: A realistic scenario where a long custom prefix could cause database issues
        String longPrefix = "USER_DEFINED_VERY_LONG_CUSTOM_PREFIX_FOR_SPECIAL_APPLICATION_COMPONENT_SYSTEM";
        Map<String, String> levelPrefixes = new HashMap<>();
        levelPrefixes.put("1", longPrefix);
        testProject.setLevelPrefixes(levelPrefixes);

        List<Requirement> existingRequirements = new ArrayList<>();

        // When: Generating requirement ID
        String reqId = callGenerateReqId(testProject, existingRequirements, 1);

        // Then: Should be prevented from exceeding database constraints
        assertTrue(reqId.length() <= 200, "Should prevent database constraint violations");
        
        // Verify the prefix was used correctly
        String expectedReqId = longPrefix + "-001";
        assertEquals(expectedReqId, reqId);
        
        // Verify the final length is acceptable
        assertTrue(reqId.length() <= 200, "Final requirement ID should be within database constraints");
    }

    // ====================================================================
    // ENTITY VALIDATION TESTS (@NotBlank and @Size)
    // ====================================================================

    @Test
    void requirementEntity_WithBlankReqId_ShouldFailValidation() {
        // Given: A requirement with blank reqId
        Requirement requirement = new Requirement(testProject, "", "Test Title", "Test Description", testUser);

        // When: Validating the entity
        Set<ConstraintViolation<Requirement>> violations = validator.validate(requirement);

        // Then: Should have validation errors
        assertFalse(violations.isEmpty(), "Should have validation violations");
        
        boolean hasNotBlankViolation = violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("reqId") && 
                          v.getMessage().equals("Requirement ID cannot be blank"));
        assertTrue(hasNotBlankViolation, "Should have @NotBlank violation for reqId");
    }

    @Test
    void requirementEntity_WithNullReqId_ShouldFailValidation() {
        // Given: A requirement with null reqId
        Requirement requirement = new Requirement(testProject, null, "Test Title", "Test Description", testUser);

        // When: Validating the entity
        Set<ConstraintViolation<Requirement>> violations = validator.validate(requirement);

        // Then: Should have validation errors
        assertFalse(violations.isEmpty(), "Should have validation violations");
        
        boolean hasNotBlankViolation = violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("reqId") && 
                          v.getMessage().equals("Requirement ID cannot be blank"));
        assertTrue(hasNotBlankViolation, "Should have @NotBlank violation for reqId");
    }

    @Test
    void requirementEntity_WithReqIdAtBoundary_200Chars_ShouldPassValidation() {
        // Given: A requirement with reqId at exactly 200 characters
        String exactly200Chars = "A".repeat(200);
        Requirement requirement = new Requirement(testProject, exactly200Chars, "Test Title", "Test Description", testUser);

        // When: Validating the entity
        Set<ConstraintViolation<Requirement>> violations = validator.validate(requirement);

        // Then: Should pass validation (exactly 200 characters is allowed)
        assertTrue(violations.isEmpty(), "Should have no validation violations for exactly 200 characters");
    }

    @Test
    void requirementEntity_WithReqIdOverLimit_201Chars_ShouldFailValidation() {
        // Given: A requirement with reqId exceeding 200 characters
        String overLimit = "A".repeat(201);
        Requirement requirement = new Requirement(testProject, overLimit, "Test Title", "Test Description", testUser);

        // When: Validating the entity
        Set<ConstraintViolation<Requirement>> violations = validator.validate(requirement);

        // Then: Should have validation errors
        assertFalse(violations.isEmpty(), "Should have validation violations");
        
        boolean hasSizeViolation = violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("reqId") && 
                          v.getMessage().contains("cannot exceed 200 characters"));
        assertTrue(hasSizeViolation, "Should have @Size violation for reqId exceeding 200 characters");
    }

    @Test
    void requirementEntity_WithReqIdAtLimit_199Chars_ShouldPassValidation() {
        // Given: A requirement with reqId at 199 characters (safely under limit)
        String safelyUnder = "A".repeat(199);
        Requirement requirement = new Requirement(testProject, safelyUnder, "Test Title", "Test Description", testUser);

        // When: Validating the entity
        Set<ConstraintViolation<Requirement>> violations = validator.validate(requirement);

        // Then: Should pass validation
        assertTrue(violations.isEmpty(), "Should have no validation violations for 199 characters");
    }

    @Test
    void requirementEntity_WithValidReqId_ShouldPassValidation() {
        // Given: A requirement with valid reqId
        Requirement requirement = new Requirement(testProject, "VALID-REQ-001", "Test Title", "Test Description", testUser);

        // When: Validating the entity
        Set<ConstraintViolation<Requirement>> violations = validator.validate(requirement);

        // Then: Should pass validation
        assertTrue(violations.isEmpty(), "Should have no validation violations");
    }

    // ====================================================================
    // INTEGRATION TESTS - REQUIREMENT CREATION WITH VALIDATION
    // ====================================================================

    @Test
    void createRequirement_WithValidProjectAndRequest_ShouldSucceed() {
        // Given: Valid project and request
        CreateRequirementRequest request = new CreateRequirementRequest();
        request.setTitle("Test Requirement");
        request.setDescription("Test Description");
        request.setStatus("DRAFT");
        request.setPriority("MEDIUM");

        setupValidProjectMocks();
        when(requirementRepository.save(any(Requirement.class))).thenAnswer(invocation -> {
            Requirement req = invocation.getArgument(0);
            req.setId(UUID.randomUUID());
            return req;
        });

        // When: Creating requirement
        var response = requirementService.createRequirement(testProject.getId(), request);

        // Then: Should succeed
        assertNotNull(response);
        verify(requirementRepository).save(any(Requirement.class));
    }

    @Test
    void createRequirement_WithOversizedPrefixInProject_ShouldThrowException() {
        // Given: Project with oversized prefix that would cause database issues
        String oversizedPrefix = "A".repeat(200); // This would cause final ID to be 210+ chars
        Map<String, String> levelPrefixes = new HashMap<>();
        levelPrefixes.put("1", oversizedPrefix);
        testProject.setLevelPrefixes(levelPrefixes);

        CreateRequirementRequest request = new CreateRequirementRequest();
        request.setTitle("Test Requirement");
        request.setDescription("Test Description");
        request.setStatus("DRAFT");
        request.setPriority("MEDIUM");

        when(authService.getCurrentUser()).thenReturn(testUser);
        when(projectMemberRepository.findByProjectIdAndUserId(testProject.getId(), testUser.getId()))
                .thenReturn(Optional.of(testMember));
        when(projectRepository.findById(testProject.getId())).thenReturn(Optional.of(testProject));
        when(requirementRepository.findByProjectId(testProject.getId())).thenReturn(new ArrayList<>());

        // When & Then: Should throw IllegalArgumentException during requirement creation
        assertThrows(IllegalArgumentException.class, () -> {
            requirementService.createRequirement(testProject.getId(), request);
        });
    }

    // ====================================================================
    // BOUNDARY CONDITION TESTS
    // ====================================================================

    @Test
    void generateReqId_WithPrefix189Chars_ShouldSucceed() {
        // Given: A prefix at 189 characters (one before boundary)
        String justUnderBoundary = "A".repeat(189);
        Map<String, String> levelPrefixes = new HashMap<>();
        levelPrefixes.put("1", justUnderBoundary);
        testProject.setLevelPrefixes(levelPrefixes);

        List<Requirement> existingRequirements = new ArrayList<>();

        // When: Generating requirement ID
        String reqId = callGenerateReqId(testProject, existingRequirements, 1);

        // Then: Should succeed
        assertTrue(reqId.length() <= 200, "Should be within database constraints");
        assertEquals(193, reqId.length(), "Should be 189 + '-' + '001' = 193 characters");
    }

    @Test
    void generateReqId_WithExistingHighNumber_ShouldStillRespectLengthLimit() {
        // Given: A scenario where existing high numbers could cause issues
        String prefix = "PREFIX_THAT_COULD_CAUSE_OVERFLOW";
        Map<String, String> levelPrefixes = new HashMap<>();
        levelPrefixes.put("1", prefix);
        testProject.setLevelPrefixes(levelPrefixes);

        // Create existing requirements with very high numbers (simulating overflow scenario)
        Requirement existing1 = new Requirement(testProject, prefix + "-999", "Test", "Desc", testUser);
        existing1.setLevel(1);
        
        Requirement existing2 = new Requirement(testProject, prefix + "-998", "Test", "Desc", testUser);
        existing2.setLevel(1);

        List<Requirement> existingRequirements = Arrays.asList(existing1, existing2);

        // When: Generating next requirement ID
        String reqId = callGenerateReqId(testProject, existingRequirements, 1);

        // Then: Should generate valid ID and respect length limits
        assertEquals(prefix + "-1000", reqId);
        assertTrue(reqId.length() <= 200, "Should still respect length limit with 4-digit numbers");
    }

    @Test
    void generateReqId_WithVeryLargeExistingNumber_ShouldValidateFinalLength() {
        // Given: A prefix that with a large existing number approaches the limit
        String prefix = "LONG_PREFIX_THAT_COULD_BE_PROBLEMATIC_WHEN_COMBINED";
        Map<String, String> levelPrefixes = new HashMap<>();
        levelPrefixes.put("1", prefix);
        testProject.setLevelPrefixes(levelPrefixes);

        // Create requirement that brings total close to 200 chars
        String closeToLimit = prefix + "-999";
        Requirement existing = new Requirement(testProject, closeToLimit, "Test", "Desc", testUser);
        existing.setLevel(1);

        List<Requirement> existingRequirements = Arrays.asList(existing);

        // When: Generating next requirement ID
        String reqId = callGenerateReqId(testProject, existingRequirements, 1);

        // Then: Should still be under limit
        assertEquals(prefix + "-1000", reqId);
        assertTrue(reqId.length() <= 200, "Should validate final length even with large existing numbers");
    }

    // ====================================================================
    // HELPER METHODS
    // ====================================================================

    /**
     * Helper method to call the private generateReqId method via reflection
     */
    private String callGenerateReqId(Project project, List<Requirement> allRequirements, int level) {
        try {
            var method = RequirementService.class.getDeclaredMethod("generateReqId", Project.class, List.class, int.class);
            method.setAccessible(true);
            return (String) method.invoke(requirementService, project, allRequirements, level);
        } catch (java.lang.reflect.InvocationTargetException e) {
            // Unwrap the actual exception thrown by the method
            if (e.getCause() != null) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                } else {
                    throw new RuntimeException("Failed to call generateReqId via reflection", e.getCause());
                }
            } else {
                throw new RuntimeException("Failed to call generateReqId via reflection", e);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to call generateReqId via reflection", e);
        }
    }

    /**
     * Setup common mocks for valid project operations
     */
    private void setupValidProjectMocks() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(projectMemberRepository.findByProjectIdAndUserId(testProject.getId(), testUser.getId()))
                .thenReturn(Optional.of(testMember));
        when(projectRepository.findById(testProject.getId())).thenReturn(Optional.of(testProject));
        when(requirementRepository.findByProjectId(testProject.getId())).thenReturn(new ArrayList<>());
        when(historyRepository.save(any(RequirementHistory.class))).thenReturn(new RequirementHistory());
        when(linkRepository.findAllLinksForRequirement(any(UUID.class))).thenReturn(new ArrayList<>());
    }
}