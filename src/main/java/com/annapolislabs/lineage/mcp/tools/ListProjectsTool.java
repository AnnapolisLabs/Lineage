package com.annapolislabs.lineage.mcp.tools;

import com.annapolislabs.lineage.dto.response.ProjectResponse;
import com.annapolislabs.lineage.mcp.McpTool;
import com.annapolislabs.lineage.mcp.McpToolExecutionException;
import com.annapolislabs.lineage.service.ProjectService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * MCP Tool for listing available projects
 */
@Component("listProjects")
public class ListProjectsTool implements McpTool {

    private final ProjectService projectService;
    private final ObjectMapper objectMapper;

    public ListProjectsTool(ProjectService projectService, ObjectMapper objectMapper) {
        this.projectService = projectService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "list_projects";
    }

    @Override
    public String getDescription() {
        return "List all projects that the current user has access to. Use this to find the project ID " +
               "before creating requirements.";
    }

    @Override
    public JsonNode getInputSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        schema.putObject("properties");
        return schema;
    }

    @Override
    public Object execute(JsonNode arguments, Map<String, Object> context) throws McpToolExecutionException {
        List<ProjectResponse> projects = projectService.getAllProjects();
        
        List<Map<String, String>> projectList = projects.stream()
            .map(p -> {
                Map<String, String> map = new java.util.HashMap<>();
                map.put("id", p.getId().toString());
                map.put("name", p.getName());
                map.put("description", p.getDescription() != null ? p.getDescription() : "");
                return map;
            })
            .toList();
        
        return Map.of(
            "success", true,
            "projects", projectList,
            "count", projectList.size()
        );
    }
}
