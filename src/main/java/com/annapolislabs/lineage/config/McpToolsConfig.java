package com.annapolislabs.lineage.config;

import com.annapolislabs.lineage.mcp.McpTool;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Configuration for MCP tools
 */
@Configuration
public class McpToolsConfig {

    @Bean
    public Map<String, McpTool> mcpToolsMap(List<McpTool> tools) {
        System.out.println("=== Registering MCP Tools ===");
        Map<String, McpTool> toolMap = tools.stream()
                .peek(tool -> System.out.println("  Tool: " + tool.getName() + " (" + tool.getClass().getSimpleName() + ")"))
                .collect(Collectors.toMap(McpTool::getName, tool -> tool));
        System.out.println("=== Total tools registered: " + toolMap.size() + " ===");
        return toolMap;
    }
}
