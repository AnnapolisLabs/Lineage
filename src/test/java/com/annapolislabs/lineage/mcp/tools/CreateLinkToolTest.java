package com.annapolislabs.lineage.mcp.tools;

import com.annapolislabs.lineage.dto.request.CreateLinkRequest;
import com.annapolislabs.lineage.service.RequirementLinkService;
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
class CreateLinkToolTest {

    @Mock
    private RequirementLinkService linkService;

    private CreateLinkTool createLinkTool;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        createLinkTool = new CreateLinkTool(linkService, objectMapper);
    }

    @Test
    void getName_ReturnsCorrectName() {
        assertEquals("create_link", createLinkTool.getName());
    }

    @Test
    void getDescription_ReturnsDescription() {
        String description = createLinkTool.getDescription();
        assertNotNull(description);
        assertTrue(description.contains("Create a bi-directional link"));
    }

    @Test
    void getInputSchema_ReturnsValidSchema() {
        JsonNode schema = createLinkTool.getInputSchema();
        assertNotNull(schema);
        assertTrue(schema.has("properties"));
        assertTrue(schema.has("required"));
    }

    @Test
    void execute_Success() throws Exception {
        // Arrange
        UUID fromRequirementId = UUID.randomUUID();
        UUID toRequirementId = UUID.randomUUID();
        UUID linkId = UUID.randomUUID();

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("fromRequirementId", fromRequirementId.toString());
        arguments.put("toRequirementId", toRequirementId.toString());

        JsonNode argsNode = objectMapper.valueToTree(arguments);

        Map<String, Object> linkResult = new HashMap<>();
        linkResult.put("id", linkId);
        linkResult.put("fromRequirementId", fromRequirementId);
        linkResult.put("toRequirementId", toRequirementId);

        when(linkService.createLink(eq(fromRequirementId), any(CreateLinkRequest.class)))
                .thenReturn(linkResult);

        // Act
        Object result = createLinkTool.execute(argsNode, new HashMap<>());

        // Assert
        assertNotNull(result);
        assertTrue(result instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        assertEquals(true, resultMap.get("success"));
        assertEquals(linkId.toString(), resultMap.get("linkId"));
        verify(linkService).createLink(eq(fromRequirementId), any(CreateLinkRequest.class));
    }
}
