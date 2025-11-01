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

@RestController
@RequestMapping("/api")
public class RequirementController {

    private final RequirementService requirementService;

    @Autowired
    public RequirementController(RequirementService requirementService) {
        this.requirementService = requirementService;
    }

    @PostMapping("/projects/{projectId}/requirements")
    public ResponseEntity<RequirementResponse> createRequirement(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateRequirementRequest request) {
        RequirementResponse response = requirementService.createRequirement(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/projects/{projectId}/requirements")
    public ResponseEntity<List<RequirementResponse>> getRequirementsByProject(@PathVariable UUID projectId) {
        List<RequirementResponse> requirements = requirementService.getRequirementsByProject(projectId);
        return ResponseEntity.ok(requirements);
    }

    @GetMapping("/requirements/{id}")
    public ResponseEntity<RequirementResponse> getRequirementById(@PathVariable UUID id) {
        RequirementResponse response = requirementService.getRequirementById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/requirements/{id}")
    public ResponseEntity<RequirementResponse> updateRequirement(
            @PathVariable UUID id,
            @Valid @RequestBody CreateRequirementRequest request) {
        RequirementResponse response = requirementService.updateRequirement(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/requirements/{id}")
    public ResponseEntity<Void> deleteRequirement(@PathVariable UUID id) {
        requirementService.deleteRequirement(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/requirements/{id}/history")
    public ResponseEntity<List<Map<String, Object>>> getRequirementHistory(@PathVariable UUID id) {
        List<Map<String, Object>> history = requirementService.getRequirementHistory(id);
        return ResponseEntity.ok(history);
    }
}
