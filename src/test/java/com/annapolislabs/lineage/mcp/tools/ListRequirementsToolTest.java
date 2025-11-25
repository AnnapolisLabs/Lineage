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

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListRequirementsToolTest {

    @Mock
    private RequirementService requirementService;

    private ListRequirementsTool listRequirementsTool;
    private ObjectMapper objectMapper;
    private User testUser;
    private Project testProject;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        listRequirementsTool = new ListRequirementsTool(requirementService, objectMapper);

        testUser = new User("test@example.com", "hashedPassword", "Test User", UserRole.DEVELOPER);
        testUser.setId(UUID.randomUUID());

        testProject = new Project("Test Project", "Description", "TEST", testUser);
        testProject.setId(UUID.randomUUID());
    }

    @Test
    void getName_ReturnsCorrectName() {
        assertEquals("list_requirements", listRequirementsTool.getName());
    }

    @Test
    void getDescription_ReturnsDescription() {
        String description = listRequirementsTool.getDescription();
        assertNotNull(description);
        assertTrue(description.contains("List all requirements"));
    }

    @Test
    void getInputSchema_ReturnsValidSchema() {
        JsonNode schema = listRequirementsTool.getInputSchema();
        assertNotNull(schema);
        assertTrue(schema.has("properties"));
        assertTrue(schema.has("required"));
    }

    @Test
    void execute_WithRequirements_Success() throws Exception {
        // Arrange
        UUID projectId = testProject.getId();

        Requirement req1 = new Requirement(testProject, "REQ-001", "Requirement 1", "Desc 1", testUser);
        req1.setId(UUID.randomUUID());
        req1.setStatus("DRAFT");
        req1.setPriority("HIGH");
        req1.setLevel(1);

        Requirement req2 = new Requirement(testProject, "REQ-002", "Requirement 2", "Desc 2", testUser);
        req2.setId(UUID.randomUUID());
        req2.setStatus("APPROVED");
        req2.setPriority("MEDIUM");
        req2.setParent(req1);
        req2.setLevel(2);

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("projectId", projectId.toString());

        JsonNode argsNode = objectMapper.valueToTree(arguments);

        List<RequirementResponse> requirements = Arrays.asList(
                new RequirementResponse(req1),
                new RequirementResponse(req2)
        );

        when(requirementService.getRequirementsByProject(projectId)).thenReturn(requirements);

        // Act
        Object result = listRequirementsTool.execute(argsNode, new HashMap<>());

        // Assert
        assertNotNull(result);
        assertTrue(result instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        assertEquals(true, resultMap.get("success"));
        assertEquals(2, resultMap.get("count"));
        assertTrue(resultMap.get("requirements") instanceof List);
        verify(requirementService).getRequirementsByProject(projectId);
    }

    @Test
    void execute_WithNoRequirements_Success() throws Exception {
        // Arrange
        UUID projectId = testProject.getId();

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("projectId", projectId.toString());

        JsonNode argsNode = objectMapper.valueToTree(arguments);

        when(requirementService.getRequirementsByProject(projectId)).thenReturn(Collections.emptyList());

        // Act
        Object result = listRequirementsTool.execute(argsNode, new HashMap<>());

        // Assert
        assertNotNull(result);
        assertTrue(result instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        assertEquals(true, resultMap.get("success"));
        assertEquals(0, resultMap.get("count"));
        verify(requirementService).getRequirementsByProject(projectId);
    }
}
