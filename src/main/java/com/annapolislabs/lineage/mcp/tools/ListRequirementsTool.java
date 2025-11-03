package com.annapolislabs.lineage.mcp.tools;

import com.annapolislabs.lineage.dto.response.RequirementResponse;
import com.annapolislabs.lineage.mcp.McpTool;
import com.annapolislabs.lineage.mcp.McpToolExecutionException;
import com.annapolislabs.lineage.service.RequirementService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * MCP Tool for listing requirements in a project
 */
@Component("listRequirements")
public class ListRequirementsTool extends BaseToolSchemaBuilder implements McpTool {

    private static final String PROJECT_ID = "projectId";

    private final RequirementService requirementService;

    public ListRequirementsTool(RequirementService requirementService, ObjectMapper objectMapper) {
        super(objectMapper);
        this.requirementService = requirementService;
    }

    @Override
    public String getName() {
        return "list_requirements";
    }

    @Override
    public String getDescription() {
        return "List all requirements in a project. Use this to see existing requirements before creating new ones " +
               "or to establish parent-child relationships.";
    }

    @Override
    public JsonNode getInputSchema() {
        ObjectNode schema = createBaseSchema();
        ObjectNode properties = schema.putObject("properties");

        addStringProperty(properties, PROJECT_ID, "UUID of the project to list requirements from");
        addRequiredFields(schema, PROJECT_ID);

        return schema;
    }

    @Override
    public Object execute(JsonNode arguments, Map<String, Object> context) throws McpToolExecutionException {
        UUID projectId = UUID.fromString(arguments.get(PROJECT_ID).asText());

        List<RequirementResponse> requirements = requirementService.getRequirementsByProject(projectId);

        List<Map<String, String>> reqList = requirements.stream()
            .map(r -> {
                Map<String, String> map = new java.util.HashMap<>();
                map.put("id", r.getId().toString());
                map.put("reqId", r.getReqId());
                map.put("title", r.getTitle());
                map.put("status", r.getStatus());
                map.put("priority", r.getPriority());
                map.put("level", String.valueOf(r.getLevel()));
                map.put("parentId", r.getParentId() != null ? r.getParentId().toString() : "");
                return map;
            })
            .toList();
        
        return Map.of(
            "success", true,
            "requirements", reqList,
            "count", reqList.size()
        );
    }
}
