package com.annapolislabs.lineage.service;

import com.annapolislabs.lineage.entity.Project;
import com.annapolislabs.lineage.entity.Requirement;
import com.annapolislabs.lineage.entity.User;
import com.annapolislabs.lineage.repository.ProjectMemberRepository;
import com.annapolislabs.lineage.repository.ProjectRepository;
import com.annapolislabs.lineage.repository.RequirementRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ExportService {

    private final RequirementRepository requirementRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final AuthService authService;
    private final ObjectMapper objectMapper;

    @Autowired
    public ExportService(RequirementRepository requirementRepository,
                        ProjectRepository projectRepository,
                        ProjectMemberRepository projectMemberRepository,
                        AuthService authService,
                        ObjectMapper objectMapper) {
        this.requirementRepository = requirementRepository;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.authService = authService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public String exportToCsv(UUID projectId) {
        checkAccess(projectId);
        List<Requirement> requirements = requirementRepository.findByProjectId(projectId);

        StringBuilder csv = new StringBuilder();
        csv.append("REQ_ID,Title,Description,Status,Priority,Parent,Created By,Created At\n");

        for (Requirement req : requirements) {
            csv.append(escapeCsv(req.getReqId())).append(",");
            csv.append(escapeCsv(req.getTitle())).append(",");
            csv.append(escapeCsv(req.getDescription())).append(",");
            csv.append(escapeCsv(req.getStatus())).append(",");
            csv.append(escapeCsv(req.getPriority())).append(",");
            csv.append(escapeCsv(req.getParent() != null ? req.getParent().getReqId() : "")).append(",");
            csv.append(escapeCsv(req.getCreatedBy() != null ? req.getCreatedBy().getEmail() : "")).append(",");
            csv.append(escapeCsv(req.getCreatedAt().toString())).append("\n");
        }

        return csv.toString();
    }

    @Transactional(readOnly = true)
    public String exportToJson(UUID projectId) {
        checkAccess(projectId);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        List<Requirement> requirements = requirementRepository.findByProjectId(projectId);

        Map<String, Object> export = new HashMap<>();
        export.put("project", Map.of(
                "name", project.getName(),
                "key", project.getProjectKey(),
                "description", project.getDescription() != null ? project.getDescription() : ""
        ));

        List<Map<String, Object>> reqList = new ArrayList<>();
        for (Requirement req : requirements) {
            Map<String, Object> reqMap = new HashMap<>();
            reqMap.put("reqId", req.getReqId());
            reqMap.put("title", req.getTitle());
            reqMap.put("description", req.getDescription());
            reqMap.put("status", req.getStatus());
            reqMap.put("priority", req.getPriority());
            reqMap.put("parentId", req.getParent() != null ? req.getParent().getReqId() : null);
            reqMap.put("customFields", req.getCustomFields());
            reqMap.put("createdAt", req.getCreatedAt().toString());
            reqList.add(reqMap);
        }
        export.put("requirements", reqList);

        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(export);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate JSON", e);
        }
    }

    @Transactional(readOnly = true)
    public String exportToMarkdown(UUID projectId) {
        checkAccess(projectId);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        List<Requirement> requirements = requirementRepository.findByProjectIdAndParentIsNullAndDeletedAtIsNull(projectId);

        StringBuilder md = new StringBuilder();
        md.append("# ").append(project.getName()).append("\n\n");
        if (project.getDescription() != null && !project.getDescription().isEmpty()) {
            md.append(project.getDescription()).append("\n\n");
        }
        md.append("---\n\n");

        for (Requirement req : requirements) {
            appendRequirementMarkdown(md, req, 2);
        }

        return md.toString();
    }

    private void appendRequirementMarkdown(StringBuilder md, Requirement req, int level) {
        md.append("#".repeat(level)).append(" ").append(req.getReqId()).append(": ").append(req.getTitle()).append("\n\n");

        md.append("**Status:** ").append(req.getStatus()).append("  \n");
        md.append("**Priority:** ").append(req.getPriority()).append("  \n\n");

        if (req.getDescription() != null && !req.getDescription().isEmpty()) {
            md.append(req.getDescription()).append("\n\n");
        }

        md.append("---\n\n");

        // Recursively append children
        List<Requirement> children = requirementRepository.findByParentIdAndDeletedAtIsNull(req.getId());
        for (Requirement child : children) {
            appendRequirementMarkdown(md, child, level + 1);
        }
    }

    private void checkAccess(UUID projectId) {
        User currentUser = authService.getCurrentUser();
        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, currentUser.getId())) {
            throw new RuntimeException("Access denied");
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
