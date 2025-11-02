package com.annapolislabs.lineage.mcp;

import com.annapolislabs.lineage.mcp.tools.ParseRequirementsTool;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Debug test to understand parsing behavior
 */
class ParseRequirementsDebugTest {

    @Test
    void debugParseSimpleRequirements() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        ParseRequirementsTool tool = new ParseRequirementsTool(objectMapper);

        String input = """
                1. The system must authenticate users
                2. The system should support dark mode
                3. High priority: Must encrypt all data""";

        ObjectNode arguments = objectMapper.createObjectNode();
        arguments.put("text", input);

        Map<String, Object> context = new HashMap<>();
        Object result = tool.execute(arguments, context);

        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;

        @SuppressWarnings("unchecked")
        List<Map<String, String>> requirements = (List<Map<String, String>>) resultMap.get("requirements");

        System.out.println("\n=== DEBUG: Parsed " + requirements.size() + " requirements ===");
        for (int i = 0; i < requirements.size(); i++) {
            Map<String, String> req = requirements.get(i);
            System.out.println("Requirement " + (i + 1) + ":");
            System.out.println("  Title: " + req.get("title"));
            System.out.println("  Description: " + req.get("description"));
            System.out.println("  Priority: " + req.get("priority"));
            System.out.println("  Status: " + req.get("status"));
            System.out.println();
        }

        // Print what we expected
        System.out.println("Expected 3 requirements:");
        System.out.println("1. The system must authenticate users");
        System.out.println("2. The system should support dark mode");
        System.out.println("3. High priority: Must encrypt all data");

        // Add assertions
        assertEquals(3, requirements.size(), "Should parse exactly 3 requirements");
        assertNotNull(requirements.get(0).get("title"), "First requirement should have a title");
        assertNotNull(requirements.get(0).get("description"), "First requirement should have a description");
        assertEquals("DRAFT", requirements.get(0).get("status"), "All requirements should have DRAFT status");
    }
}
