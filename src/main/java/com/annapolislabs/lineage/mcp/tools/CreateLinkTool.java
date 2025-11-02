package com.annapolislabs.lineage.mcp.tools;

import com.annapolislabs.lineage.dto.request.CreateLinkRequest;
import com.annapolislabs.lineage.entity.RequirementLink;
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
        
        ObjectNode fromId = properties.putObject("fromRequirementId");
        fromId.put("type", "string");
        fromId.put("description", "UUID of the source requirement");
        
        ObjectNode toId = properties.putObject("toRequirementId");
        toId.put("type", "string");
        toId.put("description", "UUID of the target requirement");
        
        schema.putArray("required").add("fromRequirementId").add("toRequirementId");
        
        return schema;
    }

    @Override
    public Object execute(JsonNode arguments, Map<String, Object> context) throws Exception {
        UUID fromRequirementId = UUID.fromString(arguments.get("fromRequirementId").asText());

        CreateLinkRequest request = new CreateLinkRequest();
        request.setToRequirementId(UUID.fromString(arguments.get("toRequirementId").asText()));

        Map<String, Object> linkResult = linkService.createLink(fromRequirementId, request);

        return Map.of(
            "success", true,
            "linkId", linkResult.get("id").toString(),
            "message", "Link created successfully between requirements"
        );
    }
}
