package com.annapolislabs.lineage.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

/**
 * Interface for MCP tools that can be executed by AI models
 */
public interface McpTool {
    /**
     * @return The name of the tool
     */
    String getName();

    /**
     * @return A description of what the tool does
     */
    String getDescription();

    /**
     * @return JSON schema for the tool's input parameters
     */
    JsonNode getInputSchema();

    /**
     * Execute the tool with the given arguments and context
     * @param arguments The input arguments as JSON
     * @param context Execution context (user ID, session, etc.)
     * @return The result of the tool execution
     * @throws McpToolExecutionException if tool execution fails
     */
    Object execute(JsonNode arguments, Map<String, Object> context) throws McpToolExecutionException;
}
