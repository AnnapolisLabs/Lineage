package com.annapolislabs.lineage.mcp.tools;

import com.annapolislabs.lineage.dto.response.RequirementResponse;
import com.annapolislabs.lineage.entity.Project;
import com.annapolislabs.lineage.entity.Requirement;
import com.annapolislabs.lineage.entity.User;
import com.annapolislabs.lineage.entity.UserRole;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateRequirementToolTest {

    @Mock
    private RequirementService requirementService;

    private CreateRequirementTool createRequirementTool;
    private ObjectMapper objectMapper;
    private User testUser;
    private Project testProject;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        createRequirementTool = new CreateRequirementTool(requirementService, objectMapper);

        testUser = new User("test@example.com", "hashedPassword", "Test User", UserRole.EDITOR);
        testUser.setId(UUID.randomUUID());

        testProject = new Project("Test Project", "Description", "TEST", testUser);
        testProject.setId(UUID.randomUUID());
    }

    @Test
    void getName_ReturnsCorrectName() {
        assertEquals("create_requirement", createRequirementTool.getName());
    }

    @Test
    void getDescription_ReturnsDescription() {
        String description = createRequirementTool.getDescription();
        assertNotNull(description);
        assertTrue(description.contains("Create a new requirement"));
    }

    @Test
    void getInputSchema_ReturnsValidSchema() {
        JsonNode schema = createRequirementTool.getInputSchema();
        assertNotNull(schema);
        assertTrue(schema.has("properties"));
        assertTrue(schema.has("required"));
    }

    @Test
    void execute_WithMinimalArguments_Success() throws Exception {
        // Arrange
        UUID projectId = testProject.getId();
        
        Requirement requirement = new Requirement(testProject, "REQ-001", "Test Requirement", "Test Description", testUser);
        requirement.setId(UUID.randomUUID());
        requirement.setStatus("DRAFT");
        requirement.setPriority("MEDIUM");

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("projectId", projectId.toString());
        arguments.put("title", "Test Requirement");
        arguments.put("description", "Test Description");

        JsonNode argsNode = objectMapper.valueToTree(arguments);

        RequirementResponse response = new RequirementResponse(requirement);

        when(requirementService.createRequirement(eq(projectId), any())).thenReturn(response);

        // Act
        Object result = createRequirementTool.execute(argsNode, new HashMap<>());

        // Assert
        assertNotNull(result);
        assertTrue(result instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        assertEquals(true, resultMap.get("success"));
        assertEquals(requirement.getId().toString(), resultMap.get("requirementId"));
        assertEquals("REQ-001", resultMap.get("reqId"));
        verify(requirementService).createRequirement(eq(projectId), any());
    }

    @Test
    void execute_WithAllArguments_Success() throws Exception {
        // Arrange
        UUID projectId = testProject.getId();

        Requirement parentReq = new Requirement(testProject, "REQ-001", "Parent", "Parent Desc", testUser);
        parentReq.setId(UUID.randomUUID());
        parentReq.setLevel(1);

        Requirement requirement = new Requirement(testProject, "REQ-002", "Test Requirement", "Test Description", testUser);
        requirement.setId(UUID.randomUUID());
        requirement.setStatus("APPROVED");
        requirement.setPriority("HIGH");
        requirement.setParent(parentReq);
        requirement.setLevel(2);

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("projectId", projectId.toString());
        arguments.put("title", "Test Requirement");
        arguments.put("description", "Test Description");
        arguments.put("status", "APPROVED");
        arguments.put("priority", "HIGH");
        arguments.put("parentId", parentReq.getId().toString());

        JsonNode argsNode = objectMapper.valueToTree(arguments);

        RequirementResponse response = new RequirementResponse(requirement);

        when(requirementService.createRequirement(eq(projectId), any())).thenReturn(response);

        // Act
        Object result = createRequirementTool.execute(argsNode, new HashMap<>());

        // Assert
        assertNotNull(result);
        assertTrue(result instanceof Map);
        verify(requirementService).createRequirement(eq(projectId), any());
    }
}
