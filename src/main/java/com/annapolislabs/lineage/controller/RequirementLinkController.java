package com.annapolislabs.lineage.controller;

import com.annapolislabs.lineage.dto.request.CreateLinkRequest;
import com.annapolislabs.lineage.service.RequirementLinkService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for managing traceability links between requirements.
 */
@RestController
@RequestMapping("/api")
public class RequirementLinkController {

    private final RequirementLinkService linkService;

    @Autowired
    public RequirementLinkController(RequirementLinkService linkService) {
        this.linkService = linkService;
    }

    /**
     * POST /api/requirements/{id}/links creates a directional link from the given requirement to another entity.
     * Returns 201 Created with link metadata when successful.
     */
    @PostMapping("/requirements/{id}/links")
    public ResponseEntity<Map<String, Object>> createLink(
            @PathVariable UUID id,
            @Valid @RequestBody CreateLinkRequest request) {
        Map<String, Object> response = linkService.createLink(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/requirements/{id}/links lists all inbound and outbound links tied to a requirement.
     */
    @GetMapping("/requirements/{id}/links")
    public ResponseEntity<List<Map<String, Object>>> getAllLinksForRequirement(@PathVariable UUID id) {
        List<Map<String, Object>> links = linkService.getAllLinksForRequirement(id);
        return ResponseEntity.ok(links);
    }

    /**
     * DELETE /api/links/{id} removes a specific link by UUID and returns 204 No Content.
     */
    @DeleteMapping("/links/{id}")
    public ResponseEntity<Void> deleteLink(@PathVariable UUID id) {
        linkService.deleteLink(id);
        return ResponseEntity.noContent().build();
    }
}
