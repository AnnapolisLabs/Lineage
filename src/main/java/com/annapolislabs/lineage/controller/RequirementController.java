package com.annapolislabs.lineage.controller;

import com.annapolislabs.lineage.dto.request.CreateRequirementRequest;
import com.annapolislabs.lineage.dto.response.RequirementResponse;
import com.annapolislabs.lineage.service.RequirementService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller providing CRUD and history endpoints for requirements scoped to projects.
 */
@RestController
@RequestMapping("/api")
public class RequirementController {

    private final RequirementService requirementService;

    @Autowired
    public RequirementController(RequirementService requirementService) {
        this.requirementService = requirementService;
    }

    /**
     * POST /api/projects/{projectId}/requirements creates a requirement under the specified project.
     * Returns 201 Created with the newly persisted requirement details.
     */
    @PostMapping("/projects/{projectId}/requirements")
    public ResponseEntity<RequirementResponse> createRequirement(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateRequirementRequest request) {
        RequirementResponse response = requirementService.createRequirement(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/projects/{projectId}/requirements lists every requirement associated with a project.
     *
     * @param projectId owning project identifier
     * @return 200 OK containing an ordered list of requirements
     */
    @GetMapping("/projects/{projectId}/requirements")
    public ResponseEntity<List<RequirementResponse>> getRequirementsByProject(@PathVariable UUID projectId) {
        List<RequirementResponse> requirements = requirementService.getRequirementsByProject(projectId);
        return ResponseEntity.ok(requirements);
    }

    /**
     * GET /api/requirements/{id} fetches a single requirement resource by ID.
     */
    @GetMapping("/requirements/{id}")
    public ResponseEntity<RequirementResponse> getRequirementById(@PathVariable UUID id) {
        RequirementResponse response = requirementService.getRequirementById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/requirements/{id} updates core requirement fields.
     *
     * @param id requirement identifier
     * @param request payload containing updated attributes
     * @return 200 OK with refreshed requirement data
     */
    @PutMapping("/requirements/{id}")
    public ResponseEntity<RequirementResponse> updateRequirement(
            @PathVariable UUID id,
            @Valid @RequestBody CreateRequirementRequest request) {
        RequirementResponse response = requirementService.updateRequirement(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/requirements/{id} removes the requirement and returns 204.
     */
    @DeleteMapping("/requirements/{id}")
    public ResponseEntity<Void> deleteRequirement(@PathVariable UUID id) {
        requirementService.deleteRequirement(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/requirements/{id}/history surfaces the change log for a requirement, aiding traceability.
     *
     * @param id requirement identifier
     * @return 200 OK with chronological history entries
     */
    @GetMapping("/requirements/{id}/history")
    public ResponseEntity<List<Map<String, Object>>> getRequirementHistory(@PathVariable UUID id) {
        List<Map<String, Object>> history = requirementService.getRequirementHistory(id);
        return ResponseEntity.ok(history);
    }
}
