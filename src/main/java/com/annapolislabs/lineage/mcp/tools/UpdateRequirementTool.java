package com.annapolislabs.lineage.mcp.tools;

import com.annapolislabs.lineage.dto.request.CreateRequirementRequest;
import com.annapolislabs.lineage.dto.response.RequirementResponse;
import com.annapolislabs.lineage.entity.Requirement;
import com.annapolislabs.lineage.mcp.McpTool;
import com.annapolislabs.lineage.repository.RequirementRepository;
import com.annapolislabs.lineage.service.RequirementService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * MCP Tool for updating requirements
 */
@Component("updateRequirement")
public class UpdateRequirementTool implements McpTool {

    private final RequirementService requirementService;
    private final RequirementRepository requirementRepository;
    private final ObjectMapper objectMapper;

    public UpdateRequirementTool(RequirementService requirementService,
                                RequirementRepository requirementRepository,
                                ObjectMapper objectMapper) {
        this.requirementService = requirementService;
        this.requirementRepository = requirementRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "update_requirement";
    }

    @Override
    public String getDescription() {
        return "Update an existing requirement. Use this to modify title, description, status, priority, or parent " +
               "of a requirement. Only provide fields that need to be updated.";
    }

    @Override
    public JsonNode getInputSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = schema.putObject("properties");

        ObjectNode requirementId = properties.putObject("requirementId");
        requirementId.put("type", "string");
        requirementId.put("description", "UUID of the requirement to update");

        ObjectNode title = properties.putObject("title");
        title.put("type", "string");
        title.put("description", "New title for the requirement (optional)");

        ObjectNode description = properties.putObject("description");
        description.put("type", "string");
        description.put("description", "New description for the requirement (optional)");

        ObjectNode status = properties.putObject("status");
        status.put("type", "string");
        status.put("description", "New status: DRAFT, APPROVED, IMPLEMENTED, VERIFIED, REJECTED (optional)");

        ObjectNode priority = properties.putObject("priority");
        priority.put("type", "string");
        priority.put("description", "New priority: LOW, MEDIUM, HIGH, CRITICAL (optional)");

        ObjectNode parentId = properties.putObject("parentId");
        parentId.put("type", "string");
        parentId.put("description", "New parent requirement UUID (optional, use null to remove parent)");

        schema.putArray("required").add("requirementId");

        return schema;
    }

    @Override
    public Object execute(JsonNode arguments, Map<String, Object> context) throws Exception {
        UUID requirementId = UUID.fromString(arguments.get("requirementId").asText());

        // Fetch current requirement to merge with partial updates
        Requirement current = requirementRepository.findById(requirementId)
                .orElseThrow(() -> new RuntimeException("Requirement not found: " + requirementId));

        // Build request with current values as defaults
        CreateRequirementRequest request = new CreateRequirementRequest();
        request.setTitle(current.getTitle());
        request.setDescription(current.getDescription());
        request.setStatus(current.getStatus());
        request.setPriority(current.getPriority());
        request.setCustomFields(current.getCustomFields());

        if (current.getParent() != null) {
            request.setParentId(current.getParent().getId());
        }

        // Apply updates from arguments
        if (arguments.has("title") && !arguments.get("title").isNull()) {
            request.setTitle(arguments.get("title").asText());
        }

        if (arguments.has("description") && !arguments.get("description").isNull()) {
            request.setDescription(arguments.get("description").asText());
        }

        if (arguments.has("status") && !arguments.get("status").isNull()) {
            request.setStatus(arguments.get("status").asText());
        }

        if (arguments.has("priority") && !arguments.get("priority").isNull()) {
            request.setPriority(arguments.get("priority").asText());
        }

        if (arguments.has("parentId")) {
            if (arguments.get("parentId").isNull()) {
                request.setParentId(null);
            } else {
                request.setParentId(UUID.fromString(arguments.get("parentId").asText()));
            }
        }

        RequirementResponse response = requirementService.updateRequirement(requirementId, request);

        return Map.of(
            "success", true,
            "requirementId", response.getId().toString(),
            "reqId", response.getReqId(),
            "message", "Requirement updated successfully: " + response.getReqId()
        );
    }
}
