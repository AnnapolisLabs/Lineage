package com.annapolislabs.lineage.mcp.tools;

import com.annapolislabs.lineage.mcp.McpTool;
import com.annapolislabs.lineage.service.RequirementService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * MCP Tool for deleting requirements
 */
@Component("deleteRequirement")
public class DeleteRequirementTool implements McpTool {

    private final RequirementService requirementService;
    private final ObjectMapper objectMapper;

    public DeleteRequirementTool(RequirementService requirementService, ObjectMapper objectMapper) {
        this.requirementService = requirementService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "delete_requirement";
    }

    @Override
    public String getDescription() {
        return "Delete a requirement from a project. Use this to remove obsolete, duplicate, or incorrect requirements. " +
               "Warning: This operation cannot be undone.";
    }

    @Override
    public JsonNode getInputSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = schema.putObject("properties");

        ObjectNode requirementId = properties.putObject("requirementId");
        requirementId.put("type", "string");
        requirementId.put("description", "UUID of the requirement to delete");

        schema.putArray("required").add("requirementId");

        return schema;
    }

    @Override
    public Object execute(JsonNode arguments, Map<String, Object> context) throws Exception {
        UUID requirementId = UUID.fromString(arguments.get("requirementId").asText());

        requirementService.deleteRequirement(requirementId);

        return Map.of(
            "success", true,
            "message", "Requirement deleted successfully: " + requirementId
        );
    }
}
