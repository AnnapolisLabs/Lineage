package com.annapolislabs.lineage.controller;

import com.annapolislabs.lineage.dto.request.CreateProjectRequest;
import com.annapolislabs.lineage.dto.request.ImportProjectRequest;
import com.annapolislabs.lineage.dto.response.ProjectResponse;
import com.annapolislabs.lineage.service.ProjectImportService;
import com.annapolislabs.lineage.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectImportService projectImportService;

    @Autowired
    public ProjectController(ProjectService projectService, ProjectImportService projectImportService) {
        this.projectService = projectService;
        this.projectImportService = projectImportService;
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody CreateProjectRequest request) {
        ProjectResponse response = projectService.createProject(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/import", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> importProject(@Valid @RequestBody ImportProjectRequest request) {
        ProjectImportService.ImportResult result = projectImportService.importProject(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "project", result.project(),
                "requirements", result.requirements()
        ));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> importProjectFile(@RequestPart("importFile") MultipartFile file) throws IOException {
        try {
            // Debug logging for multipart file processing
            String originalFilename = file.getOriginalFilename();
            long fileSize = file.getSize();
            String contentType = file.getContentType();
            
            System.out.println("=== MULTIPART FILE DEBUG INFO ===");
            System.out.println("Original filename: " + originalFilename);
            System.out.println("File size: " + fileSize + " bytes");
            System.out.println("Content type: " + contentType);
            System.out.println("Is empty: " + file.isEmpty());
            
            // Read the file content
            byte[] fileBytes = file.getBytes();
            System.out.println("Bytes read: " + fileBytes.length);
            
            String content = new String(fileBytes, StandardCharsets.UTF_8);
            System.out.println("Content length: " + content.length());
            System.out.println("First 200 chars: " + content.substring(0, Math.min(200, content.length())));
            System.out.println("=== END DEBUG INFO ===");
            
            ImportProjectRequest request = ProjectImportHelper.parse(content);
            return importProject(request);
        } catch (Exception ex) {
            System.out.println("MULTIPART ERROR: " + ex.getClass().getSimpleName() + " - " + ex.getMessage());
            ex.printStackTrace();
            throw ex;
        }
    }

    // Temporary test endpoint to debug JSON parsing
    @PostMapping(value = "/test-import", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> testImport(@RequestBody ImportProjectRequest request) {
        return ResponseEntity.ok("SUCCESS - Project: " + request.getProject().getName() + 
                               ", Requirements: " + (request.getRequirements() != null ? request.getRequirements().size() : 0));
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getAllProjects() {
        List<ProjectResponse> projects = projectService.getAllProjects();
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProjectById(@PathVariable UUID id) {
        ProjectResponse response = projectService.getProjectById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable UUID id,
            @Valid @RequestBody CreateProjectRequest request) {
        ProjectResponse response = projectService.updateProject(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable UUID id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }
}
