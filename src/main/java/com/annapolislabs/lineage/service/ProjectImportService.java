package com.annapolislabs.lineage.service;

import com.annapolislabs.lineage.dto.request.ImportProjectMetadata;
import com.annapolislabs.lineage.dto.request.ImportProjectRequest;
import com.annapolislabs.lineage.dto.response.ProjectResponse;
import com.annapolislabs.lineage.dto.response.RequirementResponse;
import com.annapolislabs.lineage.entity.ChangeType;
import com.annapolislabs.lineage.entity.Project;
import com.annapolislabs.lineage.entity.ProjectMember;
import com.annapolislabs.lineage.entity.ProjectRole;
import com.annapolislabs.lineage.entity.Requirement;
import com.annapolislabs.lineage.entity.RequirementHistory;
import com.annapolislabs.lineage.entity.RequirementLink;
import com.annapolislabs.lineage.entity.User;
import com.annapolislabs.lineage.exception.DuplicateKeyException;
import com.annapolislabs.lineage.exception.ResourceNotFoundException;
import com.annapolislabs.lineage.repository.ProjectMemberRepository;
import com.annapolislabs.lineage.repository.ProjectRepository;
import com.annapolislabs.lineage.repository.RequirementHistoryRepository;
import com.annapolislabs.lineage.repository.RequirementLinkRepository;
import com.annapolislabs.lineage.repository.RequirementRepository;
import com.annapolislabs.lineage.service.dto.ImportedRequirement;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProjectImportService {

    private final AuthService authService;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final RequirementRepository requirementRepository;
    private final RequirementLinkRepository requirementLinkRepository;
    private final RequirementHistoryRepository requirementHistoryRepository;

    public ProjectImportService(AuthService authService,
                                ProjectRepository projectRepository,
                                ProjectMemberRepository projectMemberRepository,
                                RequirementRepository requirementRepository,
                                RequirementLinkRepository requirementLinkRepository,
                                RequirementHistoryRepository requirementHistoryRepository) {
        this.authService = authService;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.requirementRepository = requirementRepository;
        this.requirementLinkRepository = requirementLinkRepository;
        this.requirementHistoryRepository = requirementHistoryRepository;
    }

    @Transactional
    public ImportResult importProject(@Valid ImportProjectRequest request) {
        ImportProjectMetadata metadata = request.getProject();
        if (projectRepository.existsByProjectKey(metadata.getKey())) {
            throw new DuplicateKeyException("Project key already exists: " + metadata.getKey());
        }

        User currentUser = authService.getCurrentUser();

        Project project = new Project(
                metadata.getName(),
                metadata.getDescription(),
                metadata.getKey(),
                currentUser
        );
        if (metadata.getLevelPrefixes() != null && !metadata.getLevelPrefixes().isEmpty()) {
            project.setLevelPrefixes(new HashMap<>(metadata.getLevelPrefixes()));
        }

        project = projectRepository.save(project);
        projectMemberRepository.save(new ProjectMember(project, currentUser, ProjectRole.ADMIN));

        Map<String, ImportedRequirement> imported = request.getRequirements().stream()
                .collect(Collectors.toMap(
                        req -> req.getReqId().toUpperCase(),
                        req -> new ImportedRequirement(req, req.getParentId()),
                        (existing, duplicate) -> {
                            throw new DuplicateKeyException("Duplicate requirement id in payload: " + duplicate.getPayload().getReqId());
                        },
                        LinkedHashMap::new));

        Map<String, Requirement> created = new HashMap<>();
        Set<String> visiting = new HashSet<>();
        List<Requirement> importedEntities = new ArrayList<>();

        for (ImportedRequirement importedRequirement : imported.values()) {
            Requirement requirement = createRequirement(project, importedRequirement, imported, created, visiting, currentUser);
            importedEntities.add(requirement);
        }

        ProjectResponse projectResponse = new ProjectResponse(project);
        List<RequirementResponse> requirementResponses = request.getRequirements().stream()
                .map(req -> created.get(req.getReqId().toUpperCase()))
                .filter(Objects::nonNull)
                .map(RequirementResponse::new)
                .toList();

        return new ImportResult(projectResponse, requirementResponses);
    }

    private Requirement createRequirement(Project project,
                                          ImportedRequirement importedRequirement,
                                          Map<String, ImportedRequirement> graph,
                                          Map<String, Requirement> created,
                                          Set<String> visiting,
                                          User currentUser) {
        String reqId = importedRequirement.getPayload().getReqId().toUpperCase();
        if (created.containsKey(reqId)) {
            return created.get(reqId);
        }
        if (!visiting.add(reqId)) {
            throw new IllegalStateException("Circular requirement dependency detected for reqId: " + reqId);
        }

        requirementRepository.findByReqId(importedRequirement.getPayload().getReqId())
                .ifPresent(existing -> {
                    throw new DuplicateKeyException("Requirement ID already exists: " + importedRequirement.getPayload().getReqId());
                });

        Requirement parent = null;
        String parentReqId = importedRequirement.getParentReqId();
        if (parentReqId != null) {
            parent = created.get(parentReqId.toUpperCase());
            if (parent == null) {
                ImportedRequirement parentPayload = graph.get(parentReqId.toUpperCase());
                if (parentPayload != null) {
                    parent = createRequirement(project, parentPayload, graph, created, visiting, currentUser);
                } else {
                    parent = requirementRepository.findByReqId(parentReqId)
                            .orElseThrow(() -> new ResourceNotFoundException("Parent requirement not found: " + parentReqId));
                    if (!parent.getProject().getId().equals(project.getId())) {
                        throw new ResourceNotFoundException("Parent requirement does not belong to this project: " + parentReqId);
                    }
                    created.put(parentReqId.toUpperCase(), parent);
                }
            }
        }

        Requirement requirement = new Requirement(
                project,
                importedRequirement.getPayload().getReqId(),
                importedRequirement.getPayload().getTitle(),
                importedRequirement.getPayload().getDescription(),
                currentUser
        );
        requirement.setStatus(importedRequirement.getPayload().getStatus() != null
                ? importedRequirement.getPayload().getStatus()
                : "DRAFT");
        requirement.setPriority(importedRequirement.getPayload().getPriority() != null
                ? importedRequirement.getPayload().getPriority()
                : "MEDIUM");
        requirement.setCustomFields(importedRequirement.getPayload().getCustomFields() != null
                ? new HashMap<>(importedRequirement.getPayload().getCustomFields())
                : new HashMap<>());

        if (parent != null) {
            requirement.setParent(parent);
            requirement.setLevel(parent.getLevel() + 1);
        } else {
            requirement.setLevel(1);
        }

        requirement = requirementRepository.save(requirement);

        if (parent != null) {
            RequirementLink link = new RequirementLink(parent, requirement, currentUser);
            requirementLinkRepository.save(link);
        }

        createHistoryEntry(requirement, currentUser);

        created.put(reqId, requirement);
        visiting.remove(reqId);
        return requirement;
    }

    private void createHistoryEntry(Requirement requirement, User user) {
        Map<String, Object> newValue = new HashMap<>();
        newValue.put("reqId", requirement.getReqId());
        newValue.put("title", requirement.getTitle());
        newValue.put("description", requirement.getDescription());
        newValue.put("status", requirement.getStatus());
        newValue.put("priority", requirement.getPriority());
        newValue.put("parentId", requirement.getParent() != null ? requirement.getParent().getId().toString() : null);
        newValue.put("customFields", requirement.getCustomFields());

        RequirementHistory history = new RequirementHistory(requirement, user, ChangeType.CREATED, null, newValue);
        requirementHistoryRepository.save(history);
    }

    public record ImportResult(ProjectResponse project, List<RequirementResponse> requirements) {}
}
