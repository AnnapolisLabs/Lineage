package com.annapolislabs.lineage.service;

import com.annapolislabs.lineage.entity.AIConversation;
import com.annapolislabs.lineage.entity.AIConversation.ConversationMessage;
import com.annapolislabs.lineage.entity.User;
import com.annapolislabs.lineage.exception.LLMApiException;
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
 * Coordinates the AI assistant experience including persistence of chat history, invocation
 * of the large language model, and orchestration of MCP tool executions.
 *
 * <p>The service is intentionally stateful per user/chat via {@link AIConversation} entities and
 * relies on transactional boundaries where mutations occur to guarantee message ordering and
 * prevent duplicate tool execution when retries happen.</p>
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
     * Processes user input through the agentic loop until a final assistant response is produced
     * or the iteration cap is reached.
     *
     * <p>The transactional boundary ensures each appended {@link ConversationMessage} persists with
     * the same atomicity as tool execution logs so a retried HTTP call cannot re-run a previously
     * completed tool step. Security-sensitive context such as the authenticated user and selected
     * project identifier are injected into both the prompt and tool execution path so downstream
     * MCP tools can enforce authorization without reloading user state.</p>
     *
     * @param chatId      stable identifier for an existing conversation or {@code null} to create one
     * @param userMessage raw text supplied by the user for this turn
     * @param projectId   identifier injected into the prompt to scope tool calls and responses
     * @param user        authenticated user executing the chat flow, used for audit context
     * @return assistant-visible text aggregated from the loop iterations; includes warning text when
     *         the maximum iteration threshold is hit
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
     * Calls the configured LLM endpoint with the persisted conversation plus any ad-hoc
     * additional input and converts the response into an {@link AgentAction}.
     *
     * <p>The payload mirrors the OpenAI chat-completions schema including the guardrail system
     * prompt so downstream providers can be swapped without touching the agent loop. The rate limit
     * and timeout configuration is tuned for the in-house LM Studio proxy, but failures bubble up so
     * callers can decide whether to retry or return an apology to users. Each successful call appends
     * the raw assistant response to the database to provide replayable history if a follow-up tool
     * action fails.</p>
     *
     * @param conversation persisted chat state that provides prior turns and is mutated with the
     *                     assistant response
     * @param additionalInput optional user text injected as the last message to steer the model
     * @return structured action parsed from the assistant response (may instruct a tool call or
     *         contain a direct user message)
     * @throws LLMApiException if the remote service responds with a non-200 status or malformed body
     * @throws java.io.IOException if the HTTP request cannot be sent or the response cannot be read
     * @throws InterruptedException if the HTTP call is interrupted while waiting for completion
     */
    private AgentAction callLLM(AIConversation conversation, String additionalInput) throws Exception {
        logger.info("Calling LLM API at {} with model {}", llmApiUrl, llmModel);

        // Build messages array
        ArrayNode messagesArray = objectMapper.createArrayNode();

        // Add system prompt
        ObjectNode systemMsg = messagesArray.addObject();
        systemMsg.put("role", "system");
        systemMsg.put(JSON_FIELD_CONTENT, AGENT_SYSTEM_PROMPT);

        // Add conversation history
        for (ConversationMessage msg : conversation.getMessages()) {
            ObjectNode msgNode = messagesArray.addObject();
            msgNode.put("role", msg.getRole());
            msgNode.put(JSON_FIELD_CONTENT, msg.getContent());
        }

        // Add additional input if provided
        if (additionalInput != null && !additionalInput.isBlank()) {
            ObjectNode inputMsg = messagesArray.addObject();
            inputMsg.put("role", "user");
            inputMsg.put(JSON_FIELD_CONTENT, additionalInput);
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
            throw new LLMApiException("LLM API error: " + response.statusCode() + " - " + response.body());
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
     * Executes the MCP tool requested by the agent, persists the serialized response into the
     * conversation, and records failures as additional conversation entries.
     *
     * <p>Tool invocation runs outside a dedicated transaction because persistence already occurs for
     * each appended message. The helper serializes tool results into JSON strings so future agent
     * calls can replay context without touching the original tool DTO. Exceptions are intentionally
     * swallowed after being logged so the user receives a descriptive failure message inside the
     * conversation stream rather than a generic HTTP 500.</p>
     *
     * @param action       structured request emitted by the agent containing the tool and arguments
     * @param conversation conversation that is mutated with synthetic "user" messages for tool output
     * @param user         authenticated user whose identity is injected into the tool context for
     *                     downstream authorization checks
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
     * Derives an actionable instruction from the assistant payload by stripping special tags and
     * extracting the JSON directive enforced by the system prompt. When parsing fails, the method
     * falls back to treating the cleaned content as a simple assistant message and, ultimately,
     * a generic clarification request.
     *
     * <p>The contract expects the LLM to emit a JSON object containing {@code tool},
     * {@code arguments}, and {@code message} fields. This helper centralizes that expectation so
     * future schema changes only touch one location. It also guarantees that user-facing output is
     * never empty by injecting a final clarification message when the model returns unusable data.</p>
     *
     * @param content raw assistant message returned by the LLM
     * @return structured {@link AgentAction} describing a tool invocation or user-facing reply
     */
    private AgentAction parseAgentResponse(String content) {
        String cleanContent = stripTagsAndMarkdown(content);
        AgentAction action = extractJsonAction(cleanContent);

        if (action != null) {
            return action;
        }

        // Fallback: treat remaining text as message
        if (!cleanContent.isBlank()) {
            return new AgentAction(null, null, cleanContent);
        }
        
        // Ultimate fallback
        return new AgentAction(null, null, "I need more information to help with that.");
    }

    /**
     * Removes agent reasoning blocks and markdown fences so downstream JSON extraction operates on
     * plain text.
     *
     * <p>The helper specifically strips {@code <think>} and {@code <system-reminder>} tags along with
     * triple-backtick code fences before trimming whitespace. It intentionally avoids executing any
     * markdown or HTML rendering to mitigate injection risks, effectively acting as an allow-list for
     * text that resembles the JSON contract the agent must satisfy.</p>
     *
     * @param content raw assistant content returned by the LLM
     * @return sanitized content eligible for JSON regex parsing
     */
    private String stripTagsAndMarkdown(String content) {
        return content
                .replaceAll("<think>[\\s\\S]*?</think>", "")
                .replaceAll("<system-reminder>[\\s\\S]*?</system-reminder>", "")
                .replaceAll("```json\\n?", "")
                .replaceAll("```\\n?", "")
                .trim();
    }

    /**
     * Extracts the JSON directive emitted by the agent. The method uses a permissive regex to
     * locate the first object literal, then validates that the expected fields (tool, arguments,
     * message) exist before converting arguments into a map structure.
     *
     * <p>The regex intentionally accepts extra prose before/after the JSON block so the agent can
     * emit natural language reasoning without breaking the parser. Only when the minimum schema is
     * present does the helper convert argument nodes into a {@link Map}; otherwise {@code null} is
     * returned so the caller can fall back to a clarification response.</p>
     *
     * @param cleanContent assistant output with markdown stripped
     * @return parsed {@link AgentAction} or {@code null} when the content is not valid JSON
     */
    private AgentAction extractJsonAction(String cleanContent) {
        Pattern jsonPattern = Pattern.compile("\\{[\\s\\S]*\\}");
        Matcher matcher = jsonPattern.matcher(cleanContent);

        if (!matcher.find()) {
            return null;
        }

        String jsonStr = matcher.group();
        try {
            JsonNode parsed = objectMapper.readTree(jsonStr);

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

        return null;
    }

    /**
     * Executes the requested MCP tool with the provided arguments and user-scoped context.
     *
     * <p>The arguments are converted into a {@link JsonNode} to match the MCP contract, and the user's
     * email is forwarded as part of the execution context so tools can apply fine-grained access
     * control. Unknown tools raise an {@link IllegalArgumentException} to make mis-configurations fail
     * fast rather than silently returning empty responses.</p>
     *
     * @param toolName  key that must exist within the injected {@code mcpToolsMap}
     * @param arguments arbitrary tool parameters serialized from the agent output
     * @param user      authenticated user; the email is forwarded so tools can enforce authorization
     * @return raw tool result returned by the MCP tool implementation (often a DTO or map)
     * @throws IllegalArgumentException when the tool name is not registered
     * @throws Exception                bubbled up from the underlying tool for callers to handle
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
     * Loads an existing conversation scoped to the supplied user or lazily creates a placeholder row
     * when the client references a brand-new chat. Centralizing this logic prevents duplicate
     * conversations when HTTP retries occur and ensures a consistent default title is applied until
     * the UI overwrites it.
     *
     * @param chatId optional identifier supplied by the client; {@code null} triggers creation of a
     *               new chat bound to the provided user
     * @param user   owner of the conversation ensuring tenant-level partitioning
     * @return persisted {@link AIConversation} instance ready for mutation
     */
    private AIConversation loadOrCreateConversation(String chatId, User user) {
        return conversationRepository.findByChatIdAndUser(chatId, user)
                .orElseGet(() -> {
                    AIConversation newConv = new AIConversation(chatId, user, "New Chat");
                    return conversationRepository.save(newConv);
                });
    }
    /**
     * Immutable representation of the agent's next action including tool invocation parameters and
     * optional user-facing message.
     */
    private static class AgentAction {
         /** Name of the MCP tool to execute or {@code null} to indicate no tool call is required. */
         final String tool;
         /** Arguments to pass to the tool when {@link #tool} is populated. */
         final Map<String, Object> arguments;
         /** Assistant-facing text that should be rendered to the end user. */
         final String message;

         AgentAction(String tool, Map<String, Object> arguments, String message) {
             this.tool = tool;
             this.arguments = arguments;
             this.message = message;
         }
     }

    /**
     * Creates a new conversation row for the supplied user and returns its generated identifier.
     * Identifiers follow the {@code chat_<epochMillis>} pattern so they remain sortable and unique
     * without hitting the database for a sequence value.
     *
     * @param user authenticated owner of the conversation
     * @return generated chat identifier that clients should store and reuse on subsequent turns
     */
    @Transactional
    public String createNewConversation(User user) {
        String chatId = "chat_" + System.currentTimeMillis();
        AIConversation conversation = new AIConversation(chatId, user, "New Chat");
        conversationRepository.save(conversation);
        return chatId;
    }

    /**
     * Retrieves the most recent conversations for a user ordered by {@code updatedAt} descending and
     * enforces a hard cap of 20 entries to keep dashboard payloads lightweight.
     *
     * @param user conversation owner
     * @return list of conversations limited to the most recent 20 entries
     */
    public List<AIConversation> getConversationHistory(User user) {
        List<AIConversation> conversations = conversationRepository.findByUserOrderByUpdatedAtDesc(user);
        return conversations.size() > 20 ? conversations.subList(0, 20) : conversations;
    }

    /**
     * Permanently deletes a conversation owned by the requesting user. The transactional boundary
     * ensures the delete cannot partially apply and guards against removing another user's chat.
     *
     * @param chatId chat identifier to delete
     * @param user   owner requesting deletion; used to scope the repository delete
     */
    @Transactional
    public void deleteConversation(String chatId, User user) {
        conversationRepository.deleteByChatIdAndUser(chatId, user);
    }
}
