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

    private static final String REQUIREMENT_ID = "requirementId";
    private static final String STRING_TYPE = "string";
    private static final String DESCRIPTION = "description";
    private static final String TITLE = "title";
    private static final String STATUS = "status";
    private static final String PRIORITY = "priority";
    private static final String PARENT_ID = "parentId";

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

        ObjectNode requirementId = properties.putObject(REQUIREMENT_ID);
        requirementId.put("type", STRING_TYPE);
        requirementId.put(DESCRIPTION, "UUID of the requirement to update");

        ObjectNode title = properties.putObject(TITLE);
        title.put("type", STRING_TYPE);
        title.put(DESCRIPTION, "New title for the requirement (optional)");

        ObjectNode description = properties.putObject(DESCRIPTION);
        description.put("type", STRING_TYPE);
        description.put(DESCRIPTION, "New description for the requirement (optional)");

        ObjectNode status = properties.putObject(STATUS);
        status.put("type", STRING_TYPE);
        status.put(DESCRIPTION, "New status: DRAFT, APPROVED, IMPLEMENTED, VERIFIED, REJECTED (optional)");

        ObjectNode priority = properties.putObject(PRIORITY);
        priority.put("type", STRING_TYPE);
        priority.put(DESCRIPTION, "New priority: LOW, MEDIUM, HIGH, CRITICAL (optional)");

        ObjectNode parentId = properties.putObject(PARENT_ID);
        parentId.put("type", STRING_TYPE);
        parentId.put(DESCRIPTION, "New parent requirement UUID (optional, use null to remove parent)");

        schema.putArray("required").add(REQUIREMENT_ID);

        return schema;
    }

    @Override
    public Object execute(JsonNode arguments, Map<String, Object> context) throws Exception {
        UUID requirementId = UUID.fromString(arguments.get(REQUIREMENT_ID).asText());

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
        if (arguments.has(TITLE) && !arguments.get(TITLE).isNull()) {
            request.setTitle(arguments.get(TITLE).asText());
        }

        if (arguments.has(DESCRIPTION) && !arguments.get(DESCRIPTION).isNull()) {
            request.setDescription(arguments.get(DESCRIPTION).asText());
        }

        if (arguments.has(STATUS) && !arguments.get(STATUS).isNull()) {
            request.setStatus(arguments.get(STATUS).asText());
        }

        if (arguments.has(PRIORITY) && !arguments.get(PRIORITY).isNull()) {
            request.setPriority(arguments.get(PRIORITY).asText());
        }

        if (arguments.has(PARENT_ID)) {
            if (arguments.get(PARENT_ID).isNull()) {
                request.setParentId(null);
            } else {
                request.setParentId(UUID.fromString(arguments.get(PARENT_ID).asText()));
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
