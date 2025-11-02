package com.annapolislabs.lineage.mcp.tools;

import com.annapolislabs.lineage.mcp.McpTool;
import com.annapolislabs.lineage.mcp.McpToolExecutionException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MCP Tool for parsing plain text or transcribed audio into structured requirements
 */
@Component("parseRequirements")
public class ParseRequirementsTool implements McpTool {

    private static final String TEXT = "text";
    private static final String CONTEXT = "context";
    private static final String STRING_TYPE = "string";
    private static final String DESCRIPTION = "description";
    private static final String MEDIUM = "MEDIUM";

    private final ObjectMapper objectMapper;

    public ParseRequirementsTool(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
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
        text.put(DESCRIPTION, "Plain text input containing requirements (e.g., customer interview transcript, specifications document)");

        ObjectNode context = properties.putObject(CONTEXT);
        context.put("type", STRING_TYPE);
        context.put(DESCRIPTION, "Optional context about the requirements (e.g., 'customer requirements for mobile app', 'system requirements for payment processing')");

        schema.putArray("required").add(TEXT);
        
        return schema;
    }

    @Override
    public Object execute(JsonNode arguments, Map<String, Object> context) throws McpToolExecutionException {
        String text = arguments.get(TEXT).asText();
        String reqContext = arguments.has(CONTEXT) ? arguments.get(CONTEXT).asText() : "";
        
        List<Map<String, String>> parsedRequirements = parseText(text, reqContext);
        
        return Map.of(
            "success", true,
            "requirements", parsedRequirements,
            "count", parsedRequirements.size(),
            "message", "Parsed " + parsedRequirements.size() + " requirements from input text. " +
                       "Use create_requirement tool to add each one to your project."
        );
    }

    private List<Map<String, String>> parseText(String text, String context) {
        List<Map<String, String>> requirements = new ArrayList<>();
        
        // Split by common requirement indicators
        String[] lines = text.split("\\r?\\n");
        
        StringBuilder currentReq = new StringBuilder();
        String currentTitle = null;
        String currentPriority = MEDIUM;
        
        // Patterns for requirement detection
        Pattern reqPattern = Pattern.compile("^\\s*(?:[-*•]|\\d+[.)]|REQ[-\\d]+:?|shall|must|should|will)\\s*(.+)", Pattern.CASE_INSENSITIVE);
        Pattern priorityPattern = Pattern.compile("(critical|high|medium|low)(?:\\s+priority)?:?", Pattern.CASE_INSENSITIVE);
        Pattern titlePattern = Pattern.compile("^#{1,3}\\s+(.+)$"); // Markdown headers

        for (String line : lines) {
            String originalLine = line.trim();
            if (originalLine.isEmpty()) continue;

            // Check for priority indicators
            Matcher priorityMatcher = priorityPattern.matcher(originalLine);
            if (priorityMatcher.find()) {
                currentPriority = priorityMatcher.group(1).toUpperCase();
            }

            // Check for markdown headers as titles
            Matcher titleMatcher = titlePattern.matcher(originalLine);
            if (titleMatcher.matches()) {
                currentTitle = titleMatcher.group(1);
                continue;
            }

            // Check if line starts a requirement
            Matcher reqMatcher = reqPattern.matcher(originalLine);
            if (reqMatcher.matches()) {
                // Save previous requirement if exists
                if (!currentReq.isEmpty() || currentTitle != null) {
                    addRequirement(requirements, currentTitle, currentReq.toString(), currentPriority);
                    currentReq = new StringBuilder();
                    currentTitle = null;
                }

                // Store current priority before resetting
                String detectedPriority = currentPriority;
                currentPriority = MEDIUM;

                // Start new requirement
                String content = reqMatcher.group(1).trim();

                // Remove priority keywords from content if present
                content = priorityPattern.matcher(content).replaceFirst("").trim();
                if (currentTitle == null && content.length() > 10) {
                    // Use first sentence as title
                    int dotIndex = content.indexOf('.');
                    if (dotIndex > 0 && dotIndex < 80) {
                        currentTitle = content.substring(0, dotIndex);
                        currentReq.append(content.substring(dotIndex + 1).trim());
                    } else if (content.length() < 100) {
                        currentTitle = content;
                    } else {
                        currentTitle = content.substring(0, 80) + "...";
                        currentReq.append(content);
                    }
                } else {
                    currentReq.append(content);
                }
            } else if (!currentReq.isEmpty() || currentTitle != null) {
                // Continue current requirement
                if (!currentReq.isEmpty()) currentReq.append(" ");
                currentReq.append(originalLine);
            } else {
                // Treat standalone text as a requirement
                if (originalLine.length() > 20) {
                    // Remove priority from the title/description
                    String cleanLine = priorityPattern.matcher(originalLine).replaceFirst("").trim();
                    String title = cleanLine.length() < 100 ? cleanLine : cleanLine.substring(0, 80) + "...";
                    addRequirement(requirements, title, cleanLine, currentPriority);
                    currentPriority = MEDIUM; // Reset for next requirement
                }
            }
        }

        // Add last requirement
        if (!currentReq.isEmpty() || currentTitle != null) {
            addRequirement(requirements, currentTitle, currentReq.toString(), currentPriority);
        }

        // If no requirements found, treat entire text as one requirement
        if (requirements.isEmpty() && !text.trim().isEmpty()) {
            String title = "Requirement from " + (context.isEmpty() ? "input" : context);
            addRequirement(requirements, title, text.trim(), MEDIUM);
        }
        
        return requirements;
    }

    private void addRequirement(List<Map<String, String>> requirements, String title, String description, String priority) {
        if (title == null || title.isEmpty()) {
            title = "Requirement " + (requirements.size() + 1);
        }
        
        if (description == null || description.isEmpty()) {
            description = title;
        }
        
        requirements.add(Map.of(
            "title", title,
            "description", description,
            "priority", priority,
            "status", "DRAFT"
        ));
    }
}
