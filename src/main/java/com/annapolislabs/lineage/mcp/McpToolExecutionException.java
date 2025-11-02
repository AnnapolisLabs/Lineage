package com.annapolislabs.lineage.mcp;

/**
 * Exception thrown when MCP tool execution fails
 */
public class McpToolExecutionException extends Exception {

    public McpToolExecutionException(String message) {
        super(message);
    }

    public McpToolExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
