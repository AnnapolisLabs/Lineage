package com.annapolislabs.lineage.mcp;

import com.annapolislabs.lineage.mcp.tools.ParseRequirementsTool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ParseRequirementsTool
 */
class ParseRequirementsToolTest {

    private ParseRequirementsTool tool;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        tool = new ParseRequirementsTool(objectMapper);
    }

    @Test
    void testGetName() {
        assertEquals("parse_requirements", tool.getName());
    }

    @Test
    void testGetDescription() {
        assertNotNull(tool.getDescription());
        assertTrue(tool.getDescription().toLowerCase().contains("parse"));
    }

    @Test
    void testGetInputSchema() {
        JsonNode schema = tool.getInputSchema();
        assertNotNull(schema);
        assertEquals("object", schema.get("type").asText());
        assertTrue(schema.get("properties").has("text"));
        assertTrue(schema.get("required").isArray());
    }

    @Test
    void testParseSimpleRequirements() throws Exception {
        String input = """
            1. The system must authenticate users
            2. The system should support dark mode
            3. High priority: Must encrypt all data
            """;

        ObjectNode arguments = objectMapper.createObjectNode();
        arguments.put("text", input);
        arguments.put("context", "Test requirements");

        Map<String, Object> context = new HashMap<>();
        Object result = tool.execute(arguments, context);

        assertNotNull(result);
        assertTrue(result instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;

        assertTrue((Boolean) resultMap.get("success"));
        assertTrue(resultMap.containsKey("requirements"));

        @SuppressWarnings("unchecked")
        List<Map<String, String>> requirements = (List<Map<String, String>>) resultMap.get("requirements");

        // Debug output
        System.out.println("Parsed " + requirements.size() + " requirements:");
        for (int i = 0; i < requirements.size(); i++) {
            Map<String, String> req = requirements.get(i);
            System.out.println("  " + (i+1) + ". " + req.get("title") + " [" + req.get("priority") + "]");
        }

        assertEquals(3, requirements.size(), "Should parse exactly 3 requirements");

        // Verify structure of first requirement
        Map<String, String> firstReq = requirements.get(0);
        assertTrue(firstReq.containsKey("title"));
        assertTrue(firstReq.containsKey("description"));
        assertTrue(firstReq.containsKey("priority"));
        assertTrue(firstReq.containsKey("status"));
        assertEquals("DRAFT", firstReq.get("status"));
    }

    @Test
    void testParsePriorityKeywords() throws Exception {
        String input = """
            Critical priority: System must be available 24/7
            Low priority: Add a dark theme option
            High priority: Implement user authentication
            """;

        ObjectNode arguments = objectMapper.createObjectNode();
        arguments.put("text", input);

        Map<String, Object> context = new HashMap<>();
        Object result = tool.execute(arguments, context);

        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        
        @SuppressWarnings("unchecked")
        List<Map<String, String>> requirements = (List<Map<String, String>>) resultMap.get("requirements");
        
        assertTrue(requirements.size() >= 3);
        
        // Check that priorities were detected
        boolean hasCritical = requirements.stream()
            .anyMatch(r -> "CRITICAL".equals(r.get("priority")));
        boolean hasLow = requirements.stream()
            .anyMatch(r -> "LOW".equals(r.get("priority")));
        boolean hasHigh = requirements.stream()
            .anyMatch(r -> "HIGH".equals(r.get("priority")));
        
        assertTrue(hasCritical || hasLow || hasHigh, "Should detect at least one priority keyword");
    }

    @Test
    void testParseMarkdownFormat() throws Exception {
        String input = """
            ## Authentication Requirements
            
            - The system shall authenticate users via email/password
            - Must support OAuth2 integration
            - Should implement 2FA for admin accounts
            
            ## Data Requirements
            
            - Critical: All data must be encrypted at rest
            """;

        ObjectNode arguments = objectMapper.createObjectNode();
        arguments.put("text", input);

        Map<String, Object> context = new HashMap<>();
        Object result = tool.execute(arguments, context);

        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        
        assertTrue((Boolean) resultMap.get("success"));
        
        @SuppressWarnings("unchecked")
        List<Map<String, String>> requirements = (List<Map<String, String>>) resultMap.get("requirements");
        
        assertTrue(requirements.size() >= 4, "Should parse requirements from markdown format");
    }

    @Test
    void testParseEmptyInput() throws Exception {
        ObjectNode arguments = objectMapper.createObjectNode();
        arguments.put("text", "");

        Map<String, Object> context = new HashMap<>();
        Object result = tool.execute(arguments, context);

        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        
        assertTrue((Boolean) resultMap.get("success"));
        
        @SuppressWarnings("unchecked")
        List<Map<String, String>> requirements = (List<Map<String, String>>) resultMap.get("requirements");
        
        assertEquals(0, requirements.size());
    }

    @Test
    void testParsePlainTextParagraph() throws Exception {
        String input = "The system must provide secure authentication for all users and log all access attempts for security auditing purposes.";

        ObjectNode arguments = objectMapper.createObjectNode();
        arguments.put("text", input);

        Map<String, Object> context = new HashMap<>();
        Object result = tool.execute(arguments, context);

        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        
        assertTrue((Boolean) resultMap.get("success"));
        
        @SuppressWarnings("unchecked")
        List<Map<String, String>> requirements = (List<Map<String, String>>) resultMap.get("requirements");
        
        assertTrue(requirements.size() >= 1, "Should create at least one requirement from plain text");
    }
}
