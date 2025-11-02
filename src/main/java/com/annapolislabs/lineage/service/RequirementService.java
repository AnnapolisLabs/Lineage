package com.annapolislabs.lineage.service;

import com.annapolislabs.lineage.dto.request.CreateRequirementRequest;
import com.annapolislabs.lineage.dto.response.RequirementResponse;
import com.annapolislabs.lineage.entity.*;
import com.annapolislabs.lineage.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class RequirementService {

    private final RequirementRepository requirementRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final RequirementHistoryRepository historyRepository;
    private final RequirementLinkRepository linkRepository;
    private final AuthService authService;

    @Autowired
    public RequirementService(RequirementRepository requirementRepository,
                             ProjectRepository projectRepository,
                             ProjectMemberRepository projectMemberRepository,
                             RequirementHistoryRepository historyRepository,
                             RequirementLinkRepository linkRepository,
                             AuthService authService) {
        this.requirementRepository = requirementRepository;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.historyRepository = historyRepository;
        this.linkRepository = linkRepository;
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

        // Get all existing requirements in the project for numbering
        List<Requirement> allRequirements = requirementRepository.findByProjectId(projectId);

        // Determine parent and generate hierarchical ID
        Requirement parent = null;
        if (request.getParentId() != null) {
            parent = requirementRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("Parent requirement not found"));
        }

        // Calculate level and generate requirement ID
        int level = parent == null ? 1 : parent.getLevel() + 1;
        String reqId = generateReqId(project, allRequirements, level);

        Requirement requirement = new Requirement(project, reqId, request.getTitle(), request.getDescription(), currentUser);
        requirement.setStatus(request.getStatus());
        requirement.setPriority(request.getPriority());
        requirement.setCustomFields(request.getCustomFields());
        requirement.setParent(parent);
        requirement.setLevel(level);

        requirement = requirementRepository.save(requirement);

        // If parent is set, automatically create a link
        if (parent != null) {
            RequirementLink link = new RequirementLink(parent, requirement, currentUser);
            linkRepository.save(link);
        }

        // Create history entry
        createHistoryEntry(requirement, currentUser, ChangeType.CREATED, null, toMap(requirement));

        return toRequirementResponse(requirement);
    }

    @Transactional(readOnly = true)
    public List<RequirementResponse> getRequirementsByProject(UUID projectId) {
        User currentUser = authService.getCurrentUser();

        // Check project access
        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, currentUser.getId())) {
            throw new RuntimeException("Access denied");
        }

        return requirementRepository.findByProjectIdAndDeletedAtIsNull(projectId)
                .stream()
                .sorted((r1, r2) -> compareReqIds(r1.getReqId(), r2.getReqId()))
                .map(this::toRequirementResponse)
                .toList();
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

        return toRequirementResponse(requirement);
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

        return toRequirementResponse(requirement);
    }

    @Transactional
    public void deleteRequirement(UUID requirementId) {
        Requirement requirement = requirementRepository.findById(requirementId)
                .orElseThrow(() -> new RuntimeException("Requirement not found"));

        if (requirement.isDeleted()) {
            throw new RuntimeException("Requirement already deleted");
        }

        User currentUser = authService.getCurrentUser();
        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(requirement.getProject().getId(), currentUser.getId())
                .orElseThrow(() -> new RuntimeException("Access denied"));

        if (member.getRole() == ProjectRole.VIEWER) {
            throw new RuntimeException("Editor access required");
        }

        // Soft delete: mark as deleted but preserve the record and ID
        Map<String, Object> oldValue = toMap(requirement);

        requirement.setDeletedAt(java.time.LocalDateTime.now());
        requirement.setDeletedBy(currentUser);
        requirementRepository.save(requirement);

        // Create history entry for deletion
        Map<String, Object> newValue = new HashMap<>();
        newValue.put("deletedAt", requirement.getDeletedAt());
        newValue.put("deletedBy", currentUser.getEmail());

        createHistoryEntry(requirement, currentUser, ChangeType.DELETED, oldValue, newValue);
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
                .toList();
    }

    /**
     * Generates requirement ID based on level prefix configuration.
     * Format: PREFIX-001, PREFIX-002, etc. where PREFIX is configured per level
     * Examples: CR-001, REN-001, SYS-001
     *
     * @param project The project
     * @param allRequirements All existing requirements in the project
     * @param level The requirement level
     * @return The generated requirement ID
     */
    private String generateReqId(Project project, List<Requirement> allRequirements, int level) {
        // Get the prefix for this level, or use a default
        String prefix = project.getLevelPrefixes().getOrDefault(String.valueOf(level), "REQ-L" + level);

        // Find the max number for this prefix
        int maxNumber = allRequirements.stream()
                .filter(req -> req.getLevel() == level)
                .map(req -> req.getReqId())
                .filter(id -> id.startsWith(prefix + "-"))
                .map(id -> {
                    try {
                        return Integer.parseInt(id.substring(prefix.length() + 1));
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                })
                .max(Integer::compare)
                .orElse(0);

        return prefix + "-" + String.format("%03d", maxNumber + 1);
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

    private RequirementResponse toRequirementResponse(Requirement requirement) {
        RequirementResponse response = new RequirementResponse(requirement);

        // Calculate link counts based on hierarchical direction
        // Out links = links pointing DOWN the hierarchy (to lower/child levels)
        // In links = links pointing UP the hierarchy (to higher/parent levels)
        List<RequirementLink> allLinks = linkRepository.findAllLinksForRequirement(requirement.getId());

        int inLinkCount = 0;
        int outLinkCount = 0;

        for (RequirementLink link : allLinks) {
            Requirement otherReq;

            if (link.getFromRequirement().getId().equals(requirement.getId())) {
                otherReq = link.getToRequirement();
            } else {
                otherReq = link.getFromRequirement();
            }

            if (otherReq.getLevel() > requirement.getLevel()) {
                // Other requirement is at a lower level (child) - OUT link
                outLinkCount++;
            } else {
                // Other requirement is at a higher level (parent) - IN link
                inLinkCount++;
            }
        }

        response.setInLinkCount(inLinkCount);
        response.setOutLinkCount(outLinkCount);

        return response;
    }

    /**
     * Compares two requirement IDs for natural ordering.
     * Handles formats like "REQ-001", "CR-123", etc.
     * Extracts the numeric portion and compares numerically.
     */
    private int compareReqIds(String reqId1, String reqId2) {
        try {
            // Extract numeric portion after the last dash
            int num1 = extractNumber(reqId1);
            int num2 = extractNumber(reqId2);

            // If numbers are different, sort by number
            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }

            // If numbers are same, sort by full string (handles different prefixes)
            return reqId1.compareTo(reqId2);
        } catch (Exception e) {
            // Fallback to string comparison if parsing fails
            return reqId1.compareTo(reqId2);
        }
    }

    private int extractNumber(String reqId) {
        // Find the last dash and extract the number after it
        int lastDash = reqId.lastIndexOf('-');
        if (lastDash >= 0 && lastDash < reqId.length() - 1) {
            String numPart = reqId.substring(lastDash + 1);
            return Integer.parseInt(numPart);
        }
        return 0;
    }
}
