package com.annapolislabs.lineage.mcp;

import com.annapolislabs.lineage.entity.Project;
import com.annapolislabs.lineage.entity.User;
import com.annapolislabs.lineage.entity.UserRole;
import com.annapolislabs.lineage.repository.ProjectRepository;
import com.annapolislabs.lineage.repository.UserRepository;
import com.annapolislabs.lineage.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for MCP WebSocket server. Runs against a real Postgres container (rather than
 * H2) because the User entity's `preferences` column is mapped as Postgres `jsonb`, and creates
 * its own test user/project directly instead of depending on DataLoader-seeded data or
 * pre-existing rows, so it works against a fresh, empty database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"
})
// Close this test's Spring context (and its Hikari pool) immediately after this class finishes,
// before Testcontainers tears down the static Postgres container. Without this, the context stays
// open until JVM shutdown, by which point the container is already gone, causing Hikari to hang
// for 30s per connection trying to validate/close against a dead container.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class McpServerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgreSQL = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("lineage_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQL::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQL::getUsername);
        registry.add("spring.datasource.password", postgreSQL::getPassword);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private String jwtToken;
    private User testUser;
    @SuppressWarnings("unused")
    private Project testProject;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        testUser = userRepository.save(
                new User("mcp-test-" + suffix + "@lineage.local", "hashed-password", "MCP Test User", UserRole.ADMINISTRATOR));

        jwtToken = jwtTokenProvider.generateAccessToken(testUser);

        testProject = projectRepository.save(
                new Project("MCP Test Project", "Project used for MCP server integration tests",
                        "MCP-" + suffix, testUser));
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
