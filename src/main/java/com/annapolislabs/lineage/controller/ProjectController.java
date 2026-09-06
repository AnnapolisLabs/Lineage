package com.annapolislabs.lineage.controller;

import com.annapolislabs.lineage.dto.request.CreateProjectRequest;
import com.annapolislabs.lineage.dto.request.ImportProjectRequest;
import com.annapolislabs.lineage.dto.response.ProjectResponse;
import com.annapolislabs.lineage.service.ProjectImportService;
import com.annapolislabs.lineage.service.ProjectService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller exposing CRUD operations plus import helpers for project resources.
 */
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private static final Logger logger = LoggerFactory.getLogger(ProjectController.class);

    private final ProjectService projectService;
    private final ProjectImportService projectImportService;

    @Autowired
    public ProjectController(ProjectService projectService, ProjectImportService projectImportService) {
        this.projectService = projectService;
        this.projectImportService = projectImportService;
    }

    /**
     * POST /api/projects creates a new project owned by the authenticated user.
     * Returns 201 Created with the full {@link ProjectResponse} from {@link ProjectService}.
     */
    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody CreateProjectRequest request) {
        ProjectResponse response = projectService.createProject(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/projects/import (JSON) ingests a structured import payload and persists the project + requirements.
     *
     * @param request JSON payload containing project metadata and requirement list
     * @return 201 Created with the persisted project and requirements summary
     */
    @PostMapping(value = "/import", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> importProject(@Valid @RequestBody ImportProjectRequest request) {
        ProjectImportService.ImportResult result = projectImportService.importProject(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "project", result.project(),
                "requirements", result.requirements()
        ));
    }

    /**
     * POST /api/projects/import (multipart) accepts an uploaded JSON file and reuses the JSON import pipeline.
     *
     * @param file uploaded JSON describing the project
     * @return 201 Created mirroring the JSON import response
     * @throws IOException when file reading fails
     */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> importProjectFile(@RequestPart("importFile") MultipartFile file) throws IOException {
        try {
            String originalFilename = file.getOriginalFilename();
            long fileSize = file.getSize();
            String contentType = file.getContentType();
            
            logger.debug("Multipart file received - filename: {}, size: {} bytes, type: {}, empty: {}",
                    originalFilename, fileSize, contentType, file.isEmpty());
            
            byte[] fileBytes = file.getBytes();
            logger.debug("Bytes read: {}", fileBytes.length);
            
            String content = new String(fileBytes, StandardCharsets.UTF_8);
            logger.debug("Content length: {}, preview: {}", content.length(),
                    content.substring(0, Math.min(200, content.length())));
            
            ImportProjectRequest request = ProjectImportHelper.parse(content);
            return importProject(request);
        } catch (Exception ex) {
            logger.error("Multipart import error: {} - {}", ex.getClass().getSimpleName(), ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * GET /api/projects lists all projects visible to the caller.
     *
     * @return 200 OK with an array of {@link ProjectResponse}
     */
    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getAllProjects() {
        List<ProjectResponse> projects = projectService.getAllProjects();
        return ResponseEntity.ok(projects);
    }

    /**
     * GET /api/projects/{id} fetches a single project resource by identifier.
     *
     * @param id project identifier
     * @return 200 OK when found or propagates service-layer exceptions when not
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProjectById(@PathVariable UUID id) {
        ProjectResponse response = projectService.getProjectById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/projects/{id} updates core project attributes.
     *
     * @param id project identifier
     * @param request payload with updated fields
     * @return 200 OK containing the updated project representation
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable UUID id,
            @Valid @RequestBody CreateProjectRequest request) {
        ProjectResponse response = projectService.updateProject(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/projects/{id} removes the specified project and returns 204 No Content on success.
     *
     * @param id project identifier to delete
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable UUID id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }
}
