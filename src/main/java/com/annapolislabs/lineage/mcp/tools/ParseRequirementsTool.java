package com.annapolislabs.lineage.mcp.tools;

import com.annapolislabs.lineage.common.ServiceConstants;
import com.annapolislabs.lineage.mcp.McpTool;
import com.annapolislabs.lineage.mcp.McpToolExecutionException;
import com.annapolislabs.lineage.mcp.tools.parser.RequirementParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * MCP Tool for parsing plain text or transcribed audio into structured requirements
 */
@Component("parseRequirements")
public class ParseRequirementsTool implements McpTool {

    private static final String TEXT = "text";
    private static final String CONTEXT = "context";
    private static final String STRING_TYPE = "string";

    private final ObjectMapper objectMapper;
    private final RequirementParser parser;

    public ParseRequirementsTool(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.parser = new RequirementParser();
    }

    @Override
    public String getName() {
        return "parse_requirements";
    }

    @Override
    public String getDescription() {
        return "Parse plain text, transcribed audio, or unstructured input into a list of structured requirements. " +
               "This tool extracts requirements from natural language and prepares them for creation. " +
               "It identifies requirement statements, prioritizes them, and organizes hierarchically where possible.";
    }

    @Override
    public JsonNode getInputSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        
        ObjectNode properties = schema.putObject("properties");

        ObjectNode text = properties.putObject(TEXT);
        text.put("type", STRING_TYPE);
        text.put(ServiceConstants.DESCRIPTION, "Plain text input containing requirements (e.g., customer interview transcript, specifications document)");

        ObjectNode context = properties.putObject(CONTEXT);
        context.put("type", STRING_TYPE);
        context.put(ServiceConstants.DESCRIPTION, "Optional context about the requirements (e.g., 'customer requirements for mobile app', 'system requirements for payment processing')");

        schema.putArray("required").add(TEXT);
        
        return schema;
    }

    @Override
    public Object execute(JsonNode arguments, Map<String, Object> context) throws McpToolExecutionException {
        String text = arguments.get(TEXT).asText();
        String reqContext = arguments.has(CONTEXT) ? arguments.get(CONTEXT).asText() : "";

        List<Map<String, String>> parsedRequirements = parser.parse(text, reqContext);

        return Map.of(
            "success", true,
            "requirements", parsedRequirements,
            "count", parsedRequirements.size(),
            "message", "Parsed " + parsedRequirements.size() + " requirements from input text. " +
                       "Use create_requirement tool to add each one to your project."
        );
    }
}
