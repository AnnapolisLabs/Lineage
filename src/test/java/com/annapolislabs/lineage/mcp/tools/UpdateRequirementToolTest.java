package com.annapolislabs.lineage.mcp.tools;

import com.annapolislabs.lineage.dto.response.RequirementResponse;
import com.annapolislabs.lineage.entity.Project;
import com.annapolislabs.lineage.entity.Requirement;
import com.annapolislabs.lineage.entity.User;
import com.annapolislabs.lineage.entity.UserRole;
import com.annapolislabs.lineage.repository.RequirementRepository;
import com.annapolislabs.lineage.service.RequirementService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateRequirementToolTest {

    @Mock
    private RequirementService requirementService;

    @Mock
    private RequirementRepository requirementRepository;

    private UpdateRequirementTool updateRequirementTool;
    private ObjectMapper objectMapper;
    private User testUser;
    private Project testProject;
    private Requirement testRequirement;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        updateRequirementTool = new UpdateRequirementTool(requirementService, requirementRepository, objectMapper);
        
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
    void getName_ReturnsCorrectName() {
        assertEquals("update_requirement", updateRequirementTool.getName());
    }

    @Test
    void getDescription_ReturnsDescription() {
        String description = updateRequirementTool.getDescription();
        assertNotNull(description);
        assertTrue(description.contains("Update an existing requirement"));
    }

    @Test
    void getInputSchema_ReturnsValidSchema() {
        JsonNode schema = updateRequirementTool.getInputSchema();
        assertNotNull(schema);
        assertTrue(schema.has("properties"));
        assertTrue(schema.has("required"));
    }

    @Test
    void execute_WithPartialUpdate_Success() throws Exception {
        // Arrange
        UUID requirementId = testRequirement.getId();

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("requirementId", requirementId.toString());
        arguments.put("title", "Updated Title");
        arguments.put("status", "APPROVED");

        JsonNode argsNode = objectMapper.valueToTree(arguments);

        when(requirementRepository.findById(requirementId)).thenReturn(Optional.of(testRequirement));
        
        testRequirement.setTitle("Updated Title");
        testRequirement.setStatus("APPROVED");
        RequirementResponse response = new RequirementResponse(testRequirement);
        
        when(requirementService.updateRequirement(eq(requirementId), any())).thenReturn(response);

        // Act
        Object result = updateRequirementTool.execute(argsNode, new HashMap<>());

        // Assert
        assertNotNull(result);
        assertTrue(result instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        assertEquals(true, resultMap.get("success"));
        verify(requirementService).updateRequirement(eq(requirementId), any());
    }

    @Test
    void execute_WithAllFields_Success() throws Exception {
        // Arrange
        UUID requirementId = testRequirement.getId();
        UUID parentId = UUID.randomUUID();

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("requirementId", requirementId.toString());
        arguments.put("title", "Updated Title");
        arguments.put("description", "Updated Description");
        arguments.put("status", "APPROVED");
        arguments.put("priority", "HIGH");
        arguments.put("parentId", parentId.toString());

        JsonNode argsNode = objectMapper.valueToTree(arguments);

        when(requirementRepository.findById(requirementId)).thenReturn(Optional.of(testRequirement));
        
        testRequirement.setTitle("Updated Title");
        testRequirement.setDescription("Updated Description");
        testRequirement.setStatus("APPROVED");
        testRequirement.setPriority("HIGH");
        RequirementResponse response = new RequirementResponse(testRequirement);
        
        when(requirementService.updateRequirement(eq(requirementId), any())).thenReturn(response);

        // Act
        Object result = updateRequirementTool.execute(argsNode, new HashMap<>());

        // Assert
        assertNotNull(result);
        verify(requirementService).updateRequirement(eq(requirementId), any());
    }
}
