package com.annapolislabs.lineage.mcp.tools;

import com.annapolislabs.lineage.common.ServiceConstants;
import com.annapolislabs.lineage.dto.request.CreateRequirementRequest;
import com.annapolislabs.lineage.dto.response.RequirementResponse;
import com.annapolislabs.lineage.entity.Requirement;
import com.annapolislabs.lineage.mcp.McpTool;
import com.annapolislabs.lineage.mcp.McpToolExecutionException;
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
public class UpdateRequirementTool extends BaseToolSchemaBuilder implements McpTool {

    private static final String PARENT_ID = "parentId";

    private final RequirementService requirementService;
    private final RequirementRepository requirementRepository;

    public UpdateRequirementTool(RequirementService requirementService,
                                RequirementRepository requirementRepository,
                                ObjectMapper objectMapper) {
        super(objectMapper);
        this.requirementService = requirementService;
        this.requirementRepository = requirementRepository;
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
        ObjectNode schema = createBaseSchema();
        ObjectNode properties = schema.putObject("properties");

        addStringProperty(properties, ServiceConstants.REQUIREMENT_ID, "UUID of the requirement to update");
        addStringProperty(properties, ServiceConstants.TITLE, "New title for the requirement (optional)");
        addStringProperty(properties, ServiceConstants.DESCRIPTION, "New description for the requirement (optional)");
        addStringProperty(properties, ServiceConstants.STATUS, "New status: DRAFT, APPROVED, IMPLEMENTED, VERIFIED, REJECTED (optional)");
        addStringProperty(properties, ServiceConstants.PRIORITY, "New priority: LOW, MEDIUM, HIGH, CRITICAL (optional)");
        addStringProperty(properties, PARENT_ID, "New parent requirement UUID (optional, use null to remove parent)");

        addRequiredFields(schema, ServiceConstants.REQUIREMENT_ID);

        return schema;
    }

    @Override
    public Object execute(JsonNode arguments, Map<String, Object> context) throws McpToolExecutionException {
        UUID requirementId = UUID.fromString(arguments.get(ServiceConstants.REQUIREMENT_ID).asText());

        // Fetch current requirement to merge with partial updates
        Requirement current = requirementRepository.findById(requirementId)
                .orElseThrow(() -> new RuntimeException(ServiceConstants.REQUIREMENT_NOT_FOUND + ": " + requirementId));

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
        if (arguments.has(ServiceConstants.TITLE) && !arguments.get(ServiceConstants.TITLE).isNull()) {
            request.setTitle(arguments.get(ServiceConstants.TITLE).asText());
        }

        if (arguments.has(ServiceConstants.DESCRIPTION) && !arguments.get(ServiceConstants.DESCRIPTION).isNull()) {
            request.setDescription(arguments.get(ServiceConstants.DESCRIPTION).asText());
        }

        if (arguments.has(ServiceConstants.STATUS) && !arguments.get(ServiceConstants.STATUS).isNull()) {
            request.setStatus(arguments.get(ServiceConstants.STATUS).asText());
        }

        if (arguments.has(ServiceConstants.PRIORITY) && !arguments.get(ServiceConstants.PRIORITY).isNull()) {
            request.setPriority(arguments.get(ServiceConstants.PRIORITY).asText());
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
