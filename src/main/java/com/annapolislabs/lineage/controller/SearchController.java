package com.annapolislabs.lineage.controller;

import com.annapolislabs.lineage.dto.response.RequirementResponse;
import com.annapolislabs.lineage.entity.Requirement;
import com.annapolislabs.lineage.entity.User;
import com.annapolislabs.lineage.exception.SearchException;
import com.annapolislabs.lineage.repository.ProjectMemberRepository;
import com.annapolislabs.lineage.repository.RequirementRepository;
import com.annapolislabs.lineage.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/search")
public class SearchController {

    private final RequirementRepository requirementRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final AuthService authService;

    @Autowired
    public SearchController(RequirementRepository requirementRepository,
                           ProjectMemberRepository projectMemberRepository,
                           AuthService authService) {
        this.requirementRepository = requirementRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<List<RequirementResponse>> search(
            @PathVariable UUID projectId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority) {

        User currentUser = authService.getCurrentUser();
        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, currentUser.getId())) {
            throw new SearchException("Access denied");
        }

        List<Requirement> results;

        if (q != null && !q.trim().isEmpty()) {
            // Full-text search
            results = requirementRepository.searchByText(projectId, q);

            // Apply additional filters
            if (status != null) {
                results = results.stream()
                        .filter(r -> r.getStatus().equals(status))
                        .toList();
            }
            if (priority != null) {
                results = results.stream()
                        .filter(r -> r.getPriority().equals(priority))
                        .toList();
            }
        } else {
            // Filter-only search
            results = requirementRepository.findByFilters(projectId, status, priority);
        }

        return ResponseEntity.ok(
                results.stream()
                        .map(RequirementResponse::new)
                        .toList()
        );
    }
}
