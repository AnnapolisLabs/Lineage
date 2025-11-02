package com.annapolislabs.lineage.mcp.tools;

import com.annapolislabs.lineage.dto.request.CreateRequirementRequest;
import com.annapolislabs.lineage.dto.response.RequirementResponse;
import com.annapolislabs.lineage.mcp.McpTool;
import com.annapolislabs.lineage.service.RequirementService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * MCP Tool for creating requirements
 */
@Component("createRequirement")
public class CreateRequirementTool implements McpTool {

    private final RequirementService requirementService;
    private final ObjectMapper objectMapper;

    public CreateRequirementTool(RequirementService requirementService, ObjectMapper objectMapper) {
        this.requirementService = requirementService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "create_requirement";
    }

    @Override
    public String getDescription() {
        return "Create a new requirement in a project. Use this to add customer requirements, system requirements, " +
               "or any other type of requirement based on specifications, audio transcripts, or plain text input.";
    }

    @Override
    public JsonNode getInputSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        
        ObjectNode properties = schema.putObject("properties");
        
        ObjectNode projectId = properties.putObject("projectId");
        projectId.put("type", "string");
        projectId.put("description", "UUID of the project to create the requirement in");
        
        ObjectNode title = properties.putObject("title");
        title.put("type", "string");
        title.put("description", "Short title/summary of the requirement");
        
        ObjectNode description = properties.putObject("description");
        description.put("type", "string");
        description.put("description", "Detailed description of the requirement (supports Markdown)");
        
        ObjectNode status = properties.putObject("status");
        status.put("type", "string");
        status.put("description", "Status: DRAFT, APPROVED, IMPLEMENTED, VERIFIED, REJECTED");
        status.put("default", "DRAFT");
        
        ObjectNode priority = properties.putObject("priority");
        priority.put("type", "string");
        priority.put("description", "Priority: LOW, MEDIUM, HIGH, CRITICAL");
        priority.put("default", "MEDIUM");
        
        ObjectNode parentId = properties.putObject("parentId");
        parentId.put("type", "string");
        parentId.put("description", "Optional UUID of parent requirement for hierarchical organization");
        
        schema.putArray("required").add("projectId").add("title").add("description");
        
        return schema;
    }

    @Override
    public Object execute(JsonNode arguments, Map<String, Object> context) throws Exception {
        UUID projectId = UUID.fromString(arguments.get("projectId").asText());
        
        CreateRequirementRequest request = new CreateRequirementRequest();
        request.setTitle(arguments.get("title").asText());
        request.setDescription(arguments.get("description").asText());
        request.setStatus(arguments.has("status") ? arguments.get("status").asText() : "DRAFT");
        request.setPriority(arguments.has("priority") ? arguments.get("priority").asText() : "MEDIUM");
        
        if (arguments.has("parentId") && !arguments.get("parentId").isNull()) {
            request.setParentId(UUID.fromString(arguments.get("parentId").asText()));
        }
        
        RequirementResponse response = requirementService.createRequirement(projectId, request);
        
        return Map.of(
            "success", true,
            "requirementId", response.getId().toString(),
            "reqId", response.getReqId(),
            "message", "Requirement created successfully: " + response.getReqId()
        );
    }
}
