package com.annapolislabs.lineage.service;

import com.annapolislabs.lineage.entity.AIConversation;
import com.annapolislabs.lineage.entity.AIConversation.ConversationMessage;
import com.annapolislabs.lineage.entity.User;
import com.annapolislabs.lineage.mcp.McpTool;
import com.annapolislabs.lineage.repository.AIConversationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for AI Agent operations
 * Handles LLM integration, agentic loops, and tool execution
 */
@Service
public class AIAgentService {
    
    private static final Logger logger = LoggerFactory.getLogger(AIAgentService.class);
    private static final int MAX_ITERATIONS = 10;
    private static final String JSON_FIELD_CONTENT = "content";
    private static final String JSON_FIELD_MESSAGE = "message";
    private static final String JSON_FIELD_ARGUMENTS = "arguments";
    private static final String AGENT_SYSTEM_PROMPT = """
            You are an assistant for Lineage Requirements Management. Always respond with valid JSON.
            
            # Tools
            - list_requirements(projectId)
            - create_requirement(projectId, title, description, priority, status, parentId?)
            - update_requirement(requirementId, ...)
            - delete_requirement(requirementId)
            - create_link(fromRequirementId, toRequirementId)
            
            # Key Facts
            - Users say "REQ-010", you use UUID "id" field
            - Always list_requirements first to get UUIDs
            - "reqId" = display (REQ-010), "id" = UUID (for tools)
            
            # Response Format
            
            Use <think> tags for reasoning (will be stripped):
            <think>Your analysis</think>
            
            Then output this JSON:
            {
              "tool": "TOOL_NAME or null",
              "arguments": {...} or null,
              "message": "User message or null"
            }
            
            # Rules
            - Need to call tool? Set tool + arguments, optionally message
            - Just responding? Set message, tool/arguments = null
            - Message shown while tool executes in background
            - Be concise
            
            # Examples
            
            User: "delete REQ-012"
            <think>List first to get UUID</think>
            {"tool": "list_requirements", "arguments": {"projectId": "ID"}, "message": null}
            
            [Returns REQ-012 uuid "abc-123"]
            <think>Found it, need confirm</think>
            {"tool": null, "arguments": null, "message": "Found REQ-012 'Test'. Delete permanently?"}
            
            User: "yes"
            <think>Execute delete</think>
            {"tool": "delete_requirement", "arguments": {"requirementId": "abc-123"}, "message": "Deleting..."}
            
            [Success]
            <think>Done</think>
            {"tool": null, "arguments": null, "message": "Deleted REQ-012."}
            
            Always output valid JSON with tool, arguments, message fields.""";
    
    private final AIConversationRepository conversationRepository;
    private final ObjectMapper objectMapper;
    private final Map<String, McpTool> tools;
    private final HttpClient httpClient;
    
    @Value("${lineage.llm.api-url}")
    private String llmApiUrl;
    
    @Value("${lineage.llm.model}")
    private String llmModel;
    
    @Value("${lineage.llm.temperature:0.7}")
    private double llmTemperature;
    
    @Value("${lineage.llm.max-tokens:60000}")
    private int llmMaxTokens;
    
    public AIAgentService(
            AIConversationRepository conversationRepository,
            ObjectMapper objectMapper,
            @Qualifier("mcpToolsMap") Map<String, McpTool> tools) {
        this.conversationRepository = conversationRepository;
        this.objectMapper = objectMapper;
        this.tools = tools;
        // Create HTTP client with HTTP/1.1 for LM Studio compatibility
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(java.time.Duration.ofSeconds(30))
                .build();
    }
    
    /**
     * Process user input with agentic loop
     */
    @Transactional
    public String processMessage(String chatId, String userMessage, String projectId, User user) {
        logger.info("Processing AI message for user={}, chatId={}", user.getEmail(), chatId);
        
        // Load or create conversation
        AIConversation conversation = loadOrCreateConversation(chatId, user);
        
        // Add user message to conversation
        conversation.getMessages().add(new ConversationMessage("user", 
                "[Current project ID: " + projectId + "]\n\n" + userMessage));
        conversationRepository.save(conversation);
        
        // Run agentic loop
        StringBuilder displayMessage = new StringBuilder();
        int iterations = 0;
        String nextInput = null;
        
        try {
            while (iterations < MAX_ITERATIONS) {
                logger.debug("Agentic loop iteration {}", iterations + 1);
                
                // Call LLM
                AgentAction action = callLLM(conversation, nextInput);
                
                // Handle message to user
                if (action.message != null && !action.message.isBlank()) {
                    if (!displayMessage.isEmpty()) {
                        displayMessage.append("\n\n");
                    }
                    displayMessage.append(action.message);
                }
                
                // Handle tool execution
                if (action.tool != null && !action.tool.isBlank()) {
                    logger.info("Executing tool: {}", action.tool);
                    handleToolExecution(action, conversation, user);
                    iterations++;
                    nextInput = null;
                } else {
                    // If no tool, we're done (message was already added above)
                    break;
                }
            }
            
            if (iterations >= MAX_ITERATIONS) {
                logger.warn("Agent reached maximum iterations");
                displayMessage.append("\n\n[Agent reached maximum iteration limit]");
            }
            
            // Save final conversation state
            conversationRepository.save(conversation);
            
            return displayMessage.toString();
            
        } catch (Exception e) {
            logger.error("Error processing message: {}", e.getMessage(), e);
            return "Sorry, I encountered an error: " + e.getMessage();
        }
    }
    
    /**
     * Call LLM API
     */
    private AgentAction callLLM(AIConversation conversation, String additionalInput) throws Exception {
        logger.info("Calling LLM API at {} with model {}", llmApiUrl, llmModel);

        // Build messages array
        ArrayNode messagesArray = objectMapper.createArrayNode();

        // Add system prompt
        ObjectNode systemMsg = messagesArray.addObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", AGENT_SYSTEM_PROMPT);

        // Add conversation history
        for (ConversationMessage msg : conversation.getMessages()) {
            ObjectNode msgNode = messagesArray.addObject();
            msgNode.put("role", msg.getRole());
            msgNode.put("content", msg.getContent());
        }

        // Add additional input if provided
        if (additionalInput != null && !additionalInput.isBlank()) {
            ObjectNode inputMsg = messagesArray.addObject();
            inputMsg.put("role", "user");
            inputMsg.put("content", additionalInput);
        }

        // Build request body
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", llmModel);
        requestBody.set("messages", messagesArray);
        requestBody.put("temperature", llmTemperature);
        requestBody.put("max_tokens", llmMaxTokens);

        String requestBodyStr = requestBody.toString();
        logger.debug("Sending request with {} messages", messagesArray.size());
        logger.debug("Request body: {}", requestBodyStr);

        // Make HTTP request with timeout
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(llmApiUrl))
                .header("Content-Type", "application/json")
                .timeout(java.time.Duration.ofMinutes(5)) // 5 minute timeout for LLM response
                .POST(HttpRequest.BodyPublishers.ofString(requestBodyStr))
                .build();

        logger.info("Sending HTTP request to LLM...");
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        logger.info("LLM response received: status={}", response.statusCode());

        if (response.statusCode() != 200) {
            throw new RuntimeException("LLM API error: " + response.statusCode() + " - " + response.body());
        }

        // Parse response
        JsonNode responseJson = objectMapper.readTree(response.body());
        String content = responseJson.path("choices").get(0).path(JSON_FIELD_MESSAGE).path(JSON_FIELD_CONTENT).asText();

        logger.debug("LLM content length: {} chars", content.length());

        // Add assistant response to conversation
        conversation.getMessages().add(new ConversationMessage("assistant", content));

        // Parse agent action
        return parseAgentResponse(content);
    }
    
    /**
     * Handle tool execution and update conversation
     */
    private void handleToolExecution(AgentAction action, AIConversation conversation, User user) {
        try {
            // Execute tool
            Object toolResult = executeTool(action.tool, action.arguments, user);

            // Add tool result to conversation
            String toolResultStr = objectMapper.writeValueAsString(toolResult);
            conversation.getMessages().add(new ConversationMessage("user",
                    "Tool " + action.tool + " returned: " + toolResultStr));
            conversationRepository.save(conversation);

        } catch (Exception e) {
            logger.error("Tool execution failed: {}", e.getMessage(), e);

            // Add error to conversation
            conversation.getMessages().add(new ConversationMessage("user",
                    "Tool " + action.tool + " failed: " + e.getMessage()));
            conversationRepository.save(conversation);
        }
    }

    /**
     * Parse LLM response into structured action
     * Strips thinking tags and extracts JSON
     */
    private AgentAction parseAgentResponse(String content) {
        // Strip thinking tags and system-reminder tags
        String cleanContent = content
                .replaceAll("<think>[\\s\\S]*?</think>", "")
                .replaceAll("<system-reminder>[\\s\\S]*?</system-reminder>", "")
                .trim();
        
        // Remove markdown code blocks
        cleanContent = cleanContent
                .replaceAll("```json\\n?", "")
                .replaceAll("```\\n?", "")
                .trim();
        
        // Extract JSON - using greedy match to get complete JSON object
        Pattern jsonPattern = Pattern.compile("\\{[\\s\\S]*\\}");
        Matcher matcher = jsonPattern.matcher(cleanContent);
        
        if (matcher.find()) {
            String jsonStr = matcher.group();
            try {
                JsonNode parsed = objectMapper.readTree(jsonStr);

                // Validate structure
                if (parsed.has("tool") && parsed.has(JSON_FIELD_ARGUMENTS) && parsed.has(JSON_FIELD_MESSAGE)) {
                    String tool = parsed.path("tool").isNull() ? null : parsed.path("tool").asText();
                    Map<String, Object> arguments = parsed.path(JSON_FIELD_ARGUMENTS).isNull() ? null :
                            objectMapper.convertValue(parsed.path(JSON_FIELD_ARGUMENTS), Map.class);
                    String message = parsed.path(JSON_FIELD_MESSAGE).isNull() ? null : parsed.path(JSON_FIELD_MESSAGE).asText();

                    return new AgentAction(tool, arguments, message);
                }
            } catch (Exception e) {
                logger.error("Failed to parse JSON from LLM response: {}", jsonStr, e);
            }
        }
        
        // Fallback: treat remaining text as message
        if (!cleanContent.isBlank()) {
            return new AgentAction(null, null, cleanContent);
        }
        
        // Ultimate fallback
        return new AgentAction(null, null, "I need more information to help with that.");
    }
    
    /**
     * Execute MCP tool
     */
    private Object executeTool(String toolName, Map<String, Object> arguments, User user) throws Exception {
        McpTool tool = tools.get(toolName);
        if (tool == null) {
            throw new IllegalArgumentException("Tool not found: " + toolName);
        }
        
        // Convert arguments to JsonNode
        JsonNode argsNode = objectMapper.valueToTree(arguments);
        
        // Build context
        Map<String, Object> context = new HashMap<>();
        context.put("userId", user.getEmail());
        
        // Execute tool
        return tool.execute(argsNode, context);
    }
    
    /**
     * Load or create conversation
     */
    private AIConversation loadOrCreateConversation(String chatId, User user) {
        return conversationRepository.findByChatIdAndUser(chatId, user)
                .orElseGet(() -> {
                    AIConversation newConv = new AIConversation(chatId, user, "New Chat");
                    return conversationRepository.save(newConv);
                });
    }
    
    /**
     * Create new conversation
     */
    @Transactional
    public String createNewConversation(User user) {
        String chatId = "chat_" + System.currentTimeMillis();
        AIConversation conversation = new AIConversation(chatId, user, "New Chat");
        conversationRepository.save(conversation);
        return chatId;
    }
    
    /**
     * Get conversation history for user
     */
    public List<AIConversation> getConversationHistory(User user) {
        List<AIConversation> conversations = conversationRepository.findByUserOrderByUpdatedAtDesc(user);
        // Limit to 20 most recent
        return conversations.size() > 20 ? conversations.subList(0, 20) : conversations;
    }
    
    /**
     * Delete conversation
     */
    @Transactional
    public void deleteConversation(String chatId, User user) {
        conversationRepository.deleteByChatIdAndUser(chatId, user);
    }
    
    /**
     * Inner class for agent action
     */
    private static class AgentAction {
        final String tool;
        final Map<String, Object> arguments;
        final String message;
        
        AgentAction(String tool, Map<String, Object> arguments, String message) {
            this.tool = tool;
            this.arguments = arguments;
            this.message = message;
        }
    }
}
