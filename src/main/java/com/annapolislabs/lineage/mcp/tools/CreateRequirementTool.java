package com.annapolislabs.lineage.mcp.tools;

import com.annapolislabs.lineage.dto.request.CreateRequirementRequest;
import com.annapolislabs.lineage.dto.response.RequirementResponse;
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
 * MCP Tool for creating requirements
 */
@Component("createRequirement")
public class CreateRequirementTool extends BaseToolSchemaBuilder implements McpTool {

    private static final String PROJECT_ID = "projectId";
    private static final String TITLE = "title";
    private static final String STATUS = "status";
    private static final String PRIORITY = "priority";
    private static final String PARENT_ID = "parentId";
    private static final String DRAFT = "DRAFT";
    private static final String MEDIUM = "MEDIUM";

    private final RequirementService requirementService;

    public CreateRequirementTool(RequirementService requirementService, ObjectMapper objectMapper) {
        super(objectMapper);
        this.requirementService = requirementService;
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
        ObjectNode schema = createBaseSchema();
        ObjectNode properties = schema.putObject("properties");

        addStringProperty(properties, PROJECT_ID, "UUID of the project to create the requirement in");
        addStringProperty(properties, TITLE, "Short title/summary of the requirement");
        addStringProperty(properties, DESCRIPTION, "Detailed description of the requirement (supports Markdown)");
        addStringProperty(properties, STATUS, "Status: DRAFT, APPROVED, IMPLEMENTED, VERIFIED, REJECTED", DRAFT);
        addStringProperty(properties, PRIORITY, "Priority: LOW, MEDIUM, HIGH, CRITICAL", MEDIUM);
        addStringProperty(properties, PARENT_ID, "Optional UUID of parent requirement for hierarchical organization");

        addRequiredFields(schema, PROJECT_ID, TITLE, DESCRIPTION);

        return schema;
    }

    @Override
    public Object execute(JsonNode arguments, Map<String, Object> context) throws McpToolExecutionException {
        UUID projectId = UUID.fromString(arguments.get(PROJECT_ID).asText());

        CreateRequirementRequest request = new CreateRequirementRequest();
        request.setTitle(arguments.get(TITLE).asText());
        request.setDescription(arguments.get(DESCRIPTION).asText());
        request.setStatus(arguments.has(STATUS) ? arguments.get(STATUS).asText() : DRAFT);
        request.setPriority(arguments.has(PRIORITY) ? arguments.get(PRIORITY).asText() : MEDIUM);

        if (arguments.has(PARENT_ID) && !arguments.get(PARENT_ID).isNull()) {
            request.setParentId(UUID.fromString(arguments.get(PARENT_ID).asText()));
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
