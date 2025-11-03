package com.annapolislabs.lineage.mcp.tools;

import com.annapolislabs.lineage.dto.request.CreateLinkRequest;
import com.annapolislabs.lineage.mcp.McpTool;
import com.annapolislabs.lineage.mcp.McpToolExecutionException;
import com.annapolislabs.lineage.service.RequirementLinkService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * MCP Tool for creating links between requirements
 */
@Component("createLink")
public class CreateLinkTool extends BaseToolSchemaBuilder implements McpTool {

    private static final String FROM_REQUIREMENT_ID = "fromRequirementId";
    private static final String TO_REQUIREMENT_ID = "toRequirementId";

    private final RequirementLinkService linkService;

    public CreateLinkTool(RequirementLinkService linkService, ObjectMapper objectMapper) {
        super(objectMapper);
        this.linkService = linkService;
    }

    @Override
    public String getName() {
        return "create_link";
    }

    @Override
    public String getDescription() {
        return "Create a bi-directional link between two requirements. Use this to establish traceability " +
               "between related requirements (e.g., customer requirement to system requirement).";
    }

    @Override
    public JsonNode getInputSchema() {
        ObjectNode schema = createBaseSchema();
        ObjectNode properties = schema.putObject("properties");

        addStringProperty(properties, FROM_REQUIREMENT_ID, "UUID of the source requirement");
        addStringProperty(properties, TO_REQUIREMENT_ID, "UUID of the target requirement");

        addRequiredFields(schema, FROM_REQUIREMENT_ID, TO_REQUIREMENT_ID);

        return schema;
    }

    @Override
    public Object execute(JsonNode arguments, Map<String, Object> context) throws McpToolExecutionException {
        UUID fromRequirementId = UUID.fromString(arguments.get(FROM_REQUIREMENT_ID).asText());

        CreateLinkRequest request = new CreateLinkRequest();
        request.setToRequirementId(UUID.fromString(arguments.get(TO_REQUIREMENT_ID).asText()));

        Map<String, Object> linkResult = linkService.createLink(fromRequirementId, request);

        return Map.of(
            "success", true,
            "linkId", linkResult.get("id").toString(),
            "message", "Link created successfully between requirements"
        );
    }
}
