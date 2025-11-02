package com.annapolislabs.lineage.config;

import com.annapolislabs.lineage.mcp.McpTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(McpToolsConfig.class);

    @Bean
    public Map<String, McpTool> mcpToolsMap(List<McpTool> tools) {
        logger.info("=== Registering MCP Tools ===");
        Map<String, McpTool> toolMap = tools.stream()
                .collect(Collectors.toMap(McpTool::getName, tool -> tool));
        toolMap.forEach((name, tool) ->
                logger.info("  Tool: {} ({})", name, tool.getClass().getSimpleName()));
        logger.info("=== Total tools registered: {} ===", toolMap.size());
        return toolMap;
    }
}
