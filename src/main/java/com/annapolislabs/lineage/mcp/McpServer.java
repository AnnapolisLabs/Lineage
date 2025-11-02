package com.annapolislabs.lineage.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP (Model Context Protocol) Server for Lineage
 * Allows AI models to interact with the requirements management system
 */
@Component
public class McpServer extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(McpServer.class);
    private static final String JSONRPC = "jsonrpc";
    private static final String METHOD = "method";
    private static final String PARAMS = "params";
    private static final String NAME = "name";
    private static final String ERROR = "error";
    private static final String CODE = "code";
    private static final String MESSAGE = "message";
    private static final String RESULT = "result";
    private static final String USER_ID = "userId";
    private static final String SESSION_ID = "sessionId";

    private final ObjectMapper objectMapper;
    private final Map<String, McpTool> tools;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public McpServer(ObjectMapper objectMapper, @Qualifier("mcpToolsMap") Map<String, McpTool> tools) {
        this.objectMapper = objectMapper;
        this.tools = tools;
        logger.info("=== McpServer received tools ===");
        tools.forEach((name, tool) -> logger.info("  Key: '{}' -> {}", name, tool.getClass().getSimpleName()));
        logger.info("================================");
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        
        // Send server info on connection
        ObjectNode response = objectMapper.createObjectNode();
        response.put(JSONRPC, "2.0");
        response.put(METHOD, "server/info");

        ObjectNode params = response.putObject(PARAMS);
        params.put(NAME, "lineage-mcp-server");
        params.put("version", "1.0.0");
        params.put("protocolVersion", "2024-11-05");

        ObjectNode capabilities = params.putObject("capabilities");
        capabilities.putObject("tools");
        
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            JsonNode request = objectMapper.readTree(message.getPayload());
            String method = request.path(METHOD).asText();

            ObjectNode response = objectMapper.createObjectNode();
            response.put(JSONRPC, "2.0");

            if (request.has("id")) {
                response.set("id", request.get("id"));
            }

            switch (method) {
                case "tools/list":
                    handleToolsList(response);
                    break;

                case "tools/call":
                    handleToolCall(request, response, session);
                    break;

                default:
                    response.putObject(ERROR)
                            .put(CODE, -32601)
                            .put(MESSAGE, "Method not found: " + method);
            }

            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));

        } catch (Exception e) {
            ObjectNode errorResponse = objectMapper.createObjectNode();
            errorResponse.put(JSONRPC, "2.0");
            errorResponse.putObject(ERROR)
                    .put(CODE, -32603)
                    .put(MESSAGE, "Internal error: " + e.getMessage());

            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(errorResponse)));
        }
    }

    private void handleToolsList(ObjectNode response) {
        ArrayNode toolsArray = response.putObject(RESULT).putArray("tools");

        for (McpTool tool : tools.values()) {
            ObjectNode toolNode = toolsArray.addObject();
            toolNode.put(NAME, tool.getName());
            toolNode.put("description", tool.getDescription());
            toolNode.set("inputSchema", tool.getInputSchema());
        }
    }

    private void handleToolCall(JsonNode request, ObjectNode response, WebSocketSession session) {
        try {
            JsonNode params = request.path(PARAMS);
            String toolName = params.path(NAME).asText();
            JsonNode arguments = params.path("arguments");

            logger.debug("Looking up tool: '{}'", toolName);
            logger.debug("Available tools: {}", tools.keySet());

            McpTool tool = tools.get(toolName);
            if (tool == null) {
                response.putObject(ERROR)
                        .put(CODE, -32602)
                        .put(MESSAGE, "Tool not found: " + toolName);
                return;
            }

            // Get authentication context from session
            String userId = (String) session.getAttributes().get(USER_ID);
            Map<String, Object> context = new HashMap<>();
            context.put(USER_ID, userId);
            context.put(SESSION_ID, session.getId());

            // Set up Spring Security context for the duration of tool execution
            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            try {
                // Execute the tool
                Object result = tool.execute(arguments, context);

                // Build response
                ObjectNode resultNode = response.putObject(RESULT);
                ArrayNode contentArray = resultNode.putArray("content");

                ObjectNode contentItem = contentArray.addObject();
                contentItem.put("type", "text");
                contentItem.put("text", objectMapper.writeValueAsString(result));
            } finally {
                // Clear security context after tool execution
                SecurityContextHolder.clearContext();
            }

        } catch (Exception e) {
            response.putObject(ERROR)
                    .put(CODE, -32603)
                    .put(MESSAGE, "Tool execution failed: " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
    }
}
