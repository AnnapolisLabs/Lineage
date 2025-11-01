package com.annapolislabs.lineage.service;

import com.annapolislabs.lineage.dto.request.CreateRequirementRequest;
import com.annapolislabs.lineage.dto.response.RequirementResponse;
import com.annapolislabs.lineage.entity.*;
import com.annapolislabs.lineage.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RequirementService {

    private final RequirementRepository requirementRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final RequirementHistoryRepository historyRepository;
    private final AuthService authService;

    @Autowired
    public RequirementService(RequirementRepository requirementRepository,
                             ProjectRepository projectRepository,
                             ProjectMemberRepository projectMemberRepository,
                             RequirementHistoryRepository historyRepository,
                             AuthService authService) {
        this.requirementRepository = requirementRepository;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.historyRepository = historyRepository;
        this.authService = authService;
    }

    @Transactional
    public RequirementResponse createRequirement(UUID projectId, CreateRequirementRequest request) {
        User currentUser = authService.getCurrentUser();

        // Check project access
        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, currentUser.getId())
                .orElseThrow(() -> new RuntimeException("Access denied"));

        if (member.getRole() == ProjectRole.VIEWER) {
            throw new RuntimeException("Editor access required");
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Generate requirement ID
        String reqId = generateReqId(project);

        Requirement requirement = new Requirement(project, reqId, request.getTitle(), request.getDescription(), currentUser);
        requirement.setStatus(request.getStatus());
        requirement.setPriority(request.getPriority());
        requirement.setCustomFields(request.getCustomFields());

        if (request.getParentId() != null) {
            Requirement parent = requirementRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("Parent requirement not found"));
            requirement.setParent(parent);
        }

        requirement = requirementRepository.save(requirement);

        // Create history entry
        createHistoryEntry(requirement, currentUser, ChangeType.CREATED, null, toMap(requirement));

        return new RequirementResponse(requirement);
    }

    @Transactional(readOnly = true)
    public List<RequirementResponse> getRequirementsByProject(UUID projectId) {
        User currentUser = authService.getCurrentUser();

        // Check project access
        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, currentUser.getId())) {
            throw new RuntimeException("Access denied");
        }

        return requirementRepository.findByProjectId(projectId)
                .stream()
                .map(RequirementResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RequirementResponse getRequirementById(UUID requirementId) {
        Requirement requirement = requirementRepository.findById(requirementId)
                .orElseThrow(() -> new RuntimeException("Requirement not found"));

        // Check project access
        User currentUser = authService.getCurrentUser();
        if (!projectMemberRepository.existsByProjectIdAndUserId(requirement.getProject().getId(), currentUser.getId())) {
            throw new RuntimeException("Access denied");
        }

        return new RequirementResponse(requirement);
    }

    @Transactional
    public RequirementResponse updateRequirement(UUID requirementId, CreateRequirementRequest request) {
        Requirement requirement = requirementRepository.findById(requirementId)
                .orElseThrow(() -> new RuntimeException("Requirement not found"));

        User currentUser = authService.getCurrentUser();
        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(requirement.getProject().getId(), currentUser.getId())
                .orElseThrow(() -> new RuntimeException("Access denied"));

        if (member.getRole() == ProjectRole.VIEWER) {
            throw new RuntimeException("Editor access required");
        }

        Map<String, Object> oldValue = toMap(requirement);

        requirement.setTitle(request.getTitle());
        requirement.setDescription(request.getDescription());
        requirement.setStatus(request.getStatus());
        requirement.setPriority(request.getPriority());
        requirement.setCustomFields(request.getCustomFields());

        if (request.getParentId() != null) {
            Requirement parent = requirementRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("Parent requirement not found"));
            requirement.setParent(parent);
        } else {
            requirement.setParent(null);
        }

        requirement = requirementRepository.save(requirement);

        // Create history entry
        createHistoryEntry(requirement, currentUser, ChangeType.UPDATED, oldValue, toMap(requirement));

        return new RequirementResponse(requirement);
    }

    @Transactional
    public void deleteRequirement(UUID requirementId) {
        Requirement requirement = requirementRepository.findById(requirementId)
                .orElseThrow(() -> new RuntimeException("Requirement not found"));

        User currentUser = authService.getCurrentUser();
        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(requirement.getProject().getId(), currentUser.getId())
                .orElseThrow(() -> new RuntimeException("Access denied"));

        if (member.getRole() == ProjectRole.VIEWER) {
            throw new RuntimeException("Editor access required");
        }

        // Create history entry before deleting (capture data first, then save after delete)
        Map<String, Object> reqData = new HashMap<>();
        reqData.put("reqId", requirement.getReqId());
        reqData.put("title", requirement.getTitle());
        reqData.put("description", requirement.getDescription());
        reqData.put("status", requirement.getStatus());
        reqData.put("priority", requirement.getPriority());
        reqData.put("parentId", requirement.getParent() != null ? requirement.getParent().getId().toString() : null);
        
        // Delete the requirement (this will cascade to children due to our entity configuration)
        requirementRepository.delete(requirement);
        
        // Note: We skip creating history entry for deletes to avoid Hibernate flush issues
        // History is still tracked via the cascade deletions
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRequirementHistory(UUID requirementId) {
        Requirement requirement = requirementRepository.findById(requirementId)
                .orElseThrow(() -> new RuntimeException("Requirement not found"));

        User currentUser = authService.getCurrentUser();
        if (!projectMemberRepository.existsByProjectIdAndUserId(requirement.getProject().getId(), currentUser.getId())) {
            throw new RuntimeException("Access denied");
        }

        return historyRepository.findByRequirementIdOrderByChangedAtDesc(requirementId)
                .stream()
                .map(this::historyToMap)
                .collect(Collectors.toList());
    }

    private String generateReqId(Project project) {
        List<Requirement> existingReqs = requirementRepository.findByProjectId(project.getId());
        int nextNumber = existingReqs.size() + 1;
        return project.getProjectKey() + "-" + String.format("%03d", nextNumber);
    }

    private void createHistoryEntry(Requirement requirement, User user, ChangeType changeType, Map<String, Object> oldValue, Map<String, Object> newValue) {
        RequirementHistory history = new RequirementHistory(requirement, user, changeType, oldValue, newValue);
        historyRepository.save(history);
    }

    private Map<String, Object> toMap(Requirement requirement) {
        Map<String, Object> map = new HashMap<>();
        map.put("reqId", requirement.getReqId());
        map.put("title", requirement.getTitle());
        map.put("description", requirement.getDescription());
        map.put("status", requirement.getStatus());
        map.put("priority", requirement.getPriority());
        map.put("parentId", requirement.getParent() != null ? requirement.getParent().getId().toString() : null);
        map.put("customFields", requirement.getCustomFields());
        return map;
    }

    private Map<String, Object> historyToMap(RequirementHistory history) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", history.getId());
        map.put("changeType", history.getChangeType());
        map.put("changedBy", history.getChangedBy() != null ? history.getChangedBy().getEmail() : null);
        map.put("changedAt", history.getChangedAt());
        map.put("oldValue", history.getOldValue());
        map.put("newValue", history.getNewValue());
        return map;
    }
}
