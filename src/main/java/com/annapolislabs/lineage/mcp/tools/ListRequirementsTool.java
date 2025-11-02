package com.annapolislabs.lineage.mcp.tools;

import com.annapolislabs.lineage.dto.response.RequirementResponse;
import com.annapolislabs.lineage.mcp.McpTool;
import com.annapolislabs.lineage.service.RequirementService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * MCP Tool for listing requirements in a project
 */
@Component("listRequirements")
public class ListRequirementsTool implements McpTool {

    private final RequirementService requirementService;
    private final ObjectMapper objectMapper;

    public ListRequirementsTool(RequirementService requirementService, ObjectMapper objectMapper) {
        this.requirementService = requirementService;
        this.objectMapper = objectMapper;
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
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        
        ObjectNode properties = schema.putObject("properties");
        
        ObjectNode projectId = properties.putObject("projectId");
        projectId.put("type", "string");
        projectId.put("description", "UUID of the project to list requirements from");
        
        schema.putArray("required").add("projectId");
        
        return schema;
    }

    @Override
    public Object execute(JsonNode arguments, Map<String, Object> context) throws Exception {
        UUID projectId = UUID.fromString(arguments.get("projectId").asText());
        
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
            .collect(Collectors.toList());
        
        return Map.of(
            "success", true,
            "requirements", reqList,
            "count", reqList.size()
        );
    }
}
