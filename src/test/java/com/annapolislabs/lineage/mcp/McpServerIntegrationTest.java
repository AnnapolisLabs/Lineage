package com.annapolislabs.lineage.mcp;

import com.annapolislabs.lineage.config.DataLoader;
import com.annapolislabs.lineage.entity.Project;
import com.annapolislabs.lineage.entity.User;
import com.annapolislabs.lineage.repository.ProjectRepository;
import com.annapolislabs.lineage.repository.UserRepository;
import com.annapolislabs.lineage.security.JwtUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for MCP WebSocket server
 * Disabled due to H2/PostgreSQL compatibility issues - test manually after starting the app
 */
@Disabled("Integration test requires full application context - test manually")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class McpServerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private String jwtToken;
    private User testUser;
    private Project testProject;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = userRepository.findByEmail("admin@lineage.local")
            .orElseThrow(() -> new RuntimeException("Test user not found - ensure DataLoader ran"));
        
        jwtToken = jwtUtil.generateToken(testUser.getEmail());
        
        // Find or create test project
        testProject = projectRepository.findAll().stream()
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No test project found"));
    }

    @Test
    void testWebSocketConnection() throws Exception {
        String wsUrl = "ws://localhost:" + port + "/mcp?token=" + jwtToken;
        StandardWebSocketClient client = new StandardWebSocketClient();
        
        CompletableFuture<JsonNode> serverInfoFuture = new CompletableFuture<>();
        
        WebSocketSession session = client.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
                JsonNode response = objectMapper.readTree(message.getPayload());
                if ("server/info".equals(response.path("method").asText())) {
                    serverInfoFuture.complete(response);
                }
            }
        }, wsUrl).get(5, TimeUnit.SECONDS);

        assertNotNull(session);
        assertTrue(session.isOpen());

        // Wait for server info message
        JsonNode serverInfo = serverInfoFuture.get(5, TimeUnit.SECONDS);
        assertNotNull(serverInfo);
        assertEquals("lineage-mcp-server", serverInfo.path("params").path("name").asText());

        session.close();
    }

    @Test
    void testToolsList() throws Exception {
        String wsUrl = "ws://localhost:" + port + "/mcp?token=" + jwtToken;
        StandardWebSocketClient client = new StandardWebSocketClient();
        
        CompletableFuture<JsonNode> toolsListFuture = new CompletableFuture<>();
        
        WebSocketSession session = client.execute(new TextWebSocketHandler() {
            private boolean serverInfoReceived = false;

            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
                JsonNode response = objectMapper.readTree(message.getPayload());
                
                if ("server/info".equals(response.path("method").asText())) {
                    serverInfoReceived = true;
                    // Send tools/list request
                    String request = objectMapper.writeValueAsString(
                        objectMapper.createObjectNode()
                            .put("jsonrpc", "2.0")
                            .put("id", 1)
                            .put("method", "tools/list")
                    );
                    session.sendMessage(new TextMessage(request));
                } else if (serverInfoReceived && response.has("result")) {
                    toolsListFuture.complete(response);
                }
            }
        }, wsUrl).get(5, TimeUnit.SECONDS);

        JsonNode toolsResponse = toolsListFuture.get(5, TimeUnit.SECONDS);
        assertNotNull(toolsResponse);
        
        JsonNode tools = toolsResponse.path("result").path("tools");
        assertTrue(tools.isArray());
        assertTrue(tools.size() >= 5, "Should have at least 5 MCP tools");

        // Verify expected tools are present
        boolean hasParseReqs = false;
        boolean hasCreateReq = false;
        boolean hasListProjects = false;

        for (JsonNode tool : tools) {
            String name = tool.path("name").asText();
            if ("parse_requirements".equals(name)) hasParseReqs = true;
            if ("create_requirement".equals(name)) hasCreateReq = true;
            if ("list_projects".equals(name)) hasListProjects = true;
        }

        assertTrue(hasParseReqs, "Should have parse_requirements tool");
        assertTrue(hasCreateReq, "Should have create_requirement tool");
        assertTrue(hasListProjects, "Should have list_projects tool");

        session.close();
    }

    @Test
    void testConnectionWithoutToken() throws Exception {
        String wsUrl = "ws://localhost:" + port + "/mcp";
        StandardWebSocketClient client = new StandardWebSocketClient();
        
        // Should fail to connect without token
        assertThrows(Exception.class, () -> {
            client.execute(new TextWebSocketHandler(), wsUrl).get(5, TimeUnit.SECONDS);
        });
    }

    @Test
    void testConnectionWithInvalidToken() throws Exception {
        String wsUrl = "ws://localhost:" + port + "/mcp?token=invalid-token";
        StandardWebSocketClient client = new StandardWebSocketClient();
        
        // Should fail to connect with invalid token
        assertThrows(Exception.class, () -> {
            client.execute(new TextWebSocketHandler(), wsUrl).get(5, TimeUnit.SECONDS);
        });
    }
}
