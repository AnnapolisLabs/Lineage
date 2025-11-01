package com.annapolislabs.lineage.service;

import com.annapolislabs.lineage.dto.request.CreateLinkRequest;
import com.annapolislabs.lineage.dto.response.RequirementResponse;
import com.annapolislabs.lineage.entity.*;
import com.annapolislabs.lineage.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RequirementLinkService {

    private final RequirementLinkRepository linkRepository;
    private final RequirementRepository requirementRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final AuthService authService;

    @Autowired
    public RequirementLinkService(RequirementLinkRepository linkRepository,
                                 RequirementRepository requirementRepository,
                                 ProjectMemberRepository projectMemberRepository,
                                 AuthService authService) {
        this.linkRepository = linkRepository;
        this.requirementRepository = requirementRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.authService = authService;
    }

    @Transactional
    public Map<String, Object> createLink(UUID fromRequirementId, CreateLinkRequest request) {
        User currentUser = authService.getCurrentUser();

        Requirement fromReq = requirementRepository.findById(fromRequirementId)
                .orElseThrow(() -> new RuntimeException("From requirement not found"));
        Requirement toReq = requirementRepository.findById(request.getToRequirementId())
                .orElseThrow(() -> new RuntimeException("To requirement not found"));

        // Check project access
        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(fromReq.getProject().getId(), currentUser.getId())
                .orElseThrow(() -> new RuntimeException("Access denied"));

        if (member.getRole() == ProjectRole.VIEWER) {
            throw new RuntimeException("Editor access required");
        }

        // Check if link already exists
        List<RequirementLink> existing = linkRepository.findByFromRequirementId(fromRequirementId);
        boolean linkExists = existing.stream()
                .anyMatch(link -> link.getToRequirement().getId().equals(request.getToRequirementId()));

        if (linkExists) {
            throw new RuntimeException("Link already exists");
        }

        RequirementLink link = new RequirementLink(fromReq, toReq, currentUser);
        link = linkRepository.save(link);

        return linkToMap(link);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllLinksForRequirement(UUID requirementId) {
        Requirement requirement = requirementRepository.findById(requirementId)
                .orElseThrow(() -> new RuntimeException("Requirement not found"));

        User currentUser = authService.getCurrentUser();
        if (!projectMemberRepository.existsByProjectIdAndUserId(requirement.getProject().getId(), currentUser.getId())) {
            throw new RuntimeException("Access denied");
        }

        List<RequirementLink> links = linkRepository.findAllLinksForRequirement(requirementId);

        List<Map<String, Object>> result = new ArrayList<>();
        for (RequirementLink link : links) {
            Map<String, Object> linkMap = new HashMap<>();
            linkMap.put("id", link.getId());

            if (link.getFromRequirement().getId().equals(requirementId)) {
                // Outgoing link
                linkMap.put("direction", "outgoing");
                linkMap.put("requirement", new RequirementResponse(link.getToRequirement()));
            } else {
                // Incoming link
                linkMap.put("direction", "incoming");
                linkMap.put("requirement", new RequirementResponse(link.getFromRequirement()));
            }

            linkMap.put("createdAt", link.getCreatedAt());
            result.add(linkMap);
        }

        return result;
    }

    @Transactional
    public void deleteLink(UUID linkId) {
        RequirementLink link = linkRepository.findById(linkId)
                .orElseThrow(() -> new RuntimeException("Link not found"));

        User currentUser = authService.getCurrentUser();
        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(
                link.getFromRequirement().getProject().getId(), currentUser.getId())
                .orElseThrow(() -> new RuntimeException("Access denied"));

        if (member.getRole() == ProjectRole.VIEWER) {
            throw new RuntimeException("Editor access required");
        }

        linkRepository.delete(link);
    }

    private Map<String, Object> linkToMap(RequirementLink link) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", link.getId());
        map.put("from", new RequirementResponse(link.getFromRequirement()));
        map.put("to", new RequirementResponse(link.getToRequirement()));
        map.put("createdAt", link.getCreatedAt());
        return map;
    }
}
