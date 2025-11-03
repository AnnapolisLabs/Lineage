package com.annapolislabs.lineage.mcp.tools;

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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteRequirementToolTest {

    @Mock
    private RequirementService requirementService;

    private DeleteRequirementTool deleteRequirementTool;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        deleteRequirementTool = new DeleteRequirementTool(requirementService, objectMapper);
    }

    @Test
    void getName_ReturnsCorrectName() {
        assertEquals("delete_requirement", deleteRequirementTool.getName());
    }

    @Test
    void getDescription_ReturnsDescription() {
        String description = deleteRequirementTool.getDescription();
        assertNotNull(description);
        assertTrue(description.contains("Delete a requirement"));
    }

    @Test
    void getInputSchema_ReturnsValidSchema() {
        JsonNode schema = deleteRequirementTool.getInputSchema();
        assertNotNull(schema);
        assertTrue(schema.has("properties"));
        assertTrue(schema.has("required"));
    }

    @Test
    void execute_Success() throws Exception {
        // Arrange
        UUID requirementId = UUID.randomUUID();

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("requirementId", requirementId.toString());

        JsonNode argsNode = objectMapper.valueToTree(arguments);

        doNothing().when(requirementService).deleteRequirement(requirementId);

        // Act
        Object result = deleteRequirementTool.execute(argsNode, new HashMap<>());

        // Assert
        assertNotNull(result);
        assertTrue(result instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        assertEquals(true, resultMap.get("success"));
        assertTrue(resultMap.get("message").toString().contains("deleted successfully"));
        verify(requirementService).deleteRequirement(requirementId);
    }
}
