package com.annapolislabs.lineage.mcp.tools;

import com.annapolislabs.lineage.dto.request.CreateLinkRequest;
import com.annapolislabs.lineage.mcp.McpTool;
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
public class CreateLinkTool implements McpTool {

    private static final String FROM_REQUIREMENT_ID = "fromRequirementId";
    private static final String TO_REQUIREMENT_ID = "toRequirementId";
    private static final String STRING_TYPE = "string";
    private static final String DESCRIPTION = "description";

    private final RequirementLinkService linkService;
    private final ObjectMapper objectMapper;

    public CreateLinkTool(RequirementLinkService linkService, ObjectMapper objectMapper) {
        this.linkService = linkService;
        this.objectMapper = objectMapper;
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
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        
        ObjectNode properties = schema.putObject("properties");

        ObjectNode fromId = properties.putObject(FROM_REQUIREMENT_ID);
        fromId.put("type", STRING_TYPE);
        fromId.put(DESCRIPTION, "UUID of the source requirement");

        ObjectNode toId = properties.putObject(TO_REQUIREMENT_ID);
        toId.put("type", STRING_TYPE);
        toId.put(DESCRIPTION, "UUID of the target requirement");

        schema.putArray("required").add(FROM_REQUIREMENT_ID).add(TO_REQUIREMENT_ID);
        
        return schema;
    }

    @Override
    public Object execute(JsonNode arguments, Map<String, Object> context) throws Exception {
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
