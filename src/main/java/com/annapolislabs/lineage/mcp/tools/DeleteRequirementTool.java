package com.annapolislabs.lineage.mcp.tools;

import com.annapolislabs.lineage.mcp.McpTool;
import com.annapolislabs.lineage.mcp.McpToolExecutionException;
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
public class DeleteRequirementTool extends BaseToolSchemaBuilder implements McpTool {

    private static final String REQUIREMENT_ID = "requirementId";

    private final RequirementService requirementService;

    public DeleteRequirementTool(RequirementService requirementService, ObjectMapper objectMapper) {
        super(objectMapper);
        this.requirementService = requirementService;
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
        ObjectNode schema = createBaseSchema();
        ObjectNode properties = schema.putObject("properties");

        addStringProperty(properties, REQUIREMENT_ID, "UUID of the requirement to delete");
        addRequiredFields(schema, REQUIREMENT_ID);

        return schema;
    }

    @Override
    public Object execute(JsonNode arguments, Map<String, Object> context) throws McpToolExecutionException {
        UUID requirementId = UUID.fromString(arguments.get(REQUIREMENT_ID).asText());

        requirementService.deleteRequirement(requirementId);

        return Map.of(
            "success", true,
            "message", "Requirement deleted successfully: " + requirementId
        );
    }
}
