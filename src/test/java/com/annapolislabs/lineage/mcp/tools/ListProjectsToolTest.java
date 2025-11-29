package com.annapolislabs.lineage.mcp.tools;

import com.annapolislabs.lineage.dto.response.ProjectResponse;
import com.annapolislabs.lineage.entity.Project;
import com.annapolislabs.lineage.entity.User;
import com.annapolislabs.lineage.entity.UserRole;
import com.annapolislabs.lineage.service.ProjectService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListProjectsToolTest {

    @Mock
    private ProjectService projectService;

    private ListProjectsTool listProjectsTool;
    private ObjectMapper objectMapper;
    private User testUser;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        listProjectsTool = new ListProjectsTool(projectService, objectMapper);
        
        testUser = new User("test@example.com", "hashedPassword", "Test User", UserRole.ADMINISTRATOR);
        testUser.setId(UUID.randomUUID());
    }

    @Test
    void getName_ReturnsCorrectName() {
        assertEquals("list_projects", listProjectsTool.getName());
    }

    @Test
    void getDescription_ReturnsDescription() {
        String description = listProjectsTool.getDescription();
        assertNotNull(description);
        assertTrue(description.contains("List all projects"));
    }

    @Test
    void getInputSchema_ReturnsValidSchema() {
        JsonNode schema = listProjectsTool.getInputSchema();
        assertNotNull(schema);
        assertTrue(schema.has("properties"));
    }

    @Test
    void execute_WithProjects_Success() throws Exception {
        // Arrange
        Project project1 = new Project("Project 1", "Desc 1", "PROJ1", testUser);
        project1.setId(UUID.randomUUID());
        Project project2 = new Project("Project 2", "Desc 2", "PROJ2", testUser);
        project2.setId(UUID.randomUUID());

        List<ProjectResponse> projects = Arrays.asList(
                new ProjectResponse(project1),
                new ProjectResponse(project2)
        );

        when(projectService.getAllProjects()).thenReturn(projects);

        JsonNode argsNode = objectMapper.createObjectNode();

        // Act
        Object result = listProjectsTool.execute(argsNode, new HashMap<>());

        // Assert
        assertNotNull(result);
        assertTrue(result instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        assertEquals(true, resultMap.get("success"));
        assertEquals(2, resultMap.get("count"));
        verify(projectService).getAllProjects();
    }

    @Test
    void execute_WithNoProjects_Success() throws Exception {
        // Arrange
        when(projectService.getAllProjects()).thenReturn(Collections.emptyList());

        JsonNode argsNode = objectMapper.createObjectNode();

        // Act
        Object result = listProjectsTool.execute(argsNode, new HashMap<>());

        // Assert
        assertNotNull(result);
        assertTrue(result instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        assertEquals(true, resultMap.get("success"));
        assertEquals(0, resultMap.get("count"));
    }
}
