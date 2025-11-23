package com.annapolislabs.lineage.service;

import com.annapolislabs.lineage.entity.Project;
import com.annapolislabs.lineage.entity.Requirement;
import com.annapolislabs.lineage.entity.User;
import com.annapolislabs.lineage.exception.ExportException;
import com.annapolislabs.lineage.exception.ResourceNotFoundException;
import com.annapolislabs.lineage.repository.ProjectMemberRepository;
import com.annapolislabs.lineage.repository.ProjectRepository;
import com.annapolislabs.lineage.repository.RequirementRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Provides CSV, JSON, and Markdown exports for project requirements while enforcing membership-based
 * access control. Export formats are intentionally lightweight so they can be consumed by external
 * reporting tools without additional transformations.
 */
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

    /**
     * Generates a comma-separated export for every requirement belonging to the supplied project.
     * Access is limited to users who are members of the project and the export is intentionally
     * flattened so spreadsheets and reporting tools can consume the data without transformations.
     *
     * @param projectId identifier of the project being exported
     * @return CSV content with a fixed header row followed by one row per requirement
     * @throws ExportException if the caller lacks membership or if repository calls fail
     */
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

    /**
     * Produces a JSON payload containing project metadata and an array of requirement objects. The
     * method mirrors the public API schema so exports can be re-imported or consumed by tooling and
     * is limited to project members.
     *
     * @param projectId identifier of the project being exported
     * @return pretty-printed JSON representation of the project and its requirements
     * @throws ResourceNotFoundException when the project no longer exists
     * @throws ExportException           when serialization fails
     */
    @Transactional(readOnly = true)
    public String exportToJson(UUID projectId) {
        checkAccess(projectId);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
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
            throw new ExportException("Failed to generate JSON", e);
        }
    }

    /**
     * Renders a Markdown snapshot of the project by recursively walking the requirement hierarchy and
     * emitting heading levels that match the depth of each node. Access is limited to project members
     * and the output is optimized for documentation portals that accept GitHub-flavored markdown.
     *
     * @param projectId identifier for the project
     * @return Markdown string suitable for documentation portals
     * @throws ResourceNotFoundException when the project cannot be found
     */
    @Transactional(readOnly = true)
    public String exportToMarkdown(UUID projectId) {
        checkAccess(projectId);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
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

    /**
     * Recursively appends a requirement and all descendents to the Markdown builder with heading
     * levels reflecting their hierarchical depth.
     *
     * @param md    markdown builder shared across recursion
     * @param req   requirement currently being rendered
     * @param level heading depth (e.g., 2 => ##)
     */
    /**
     * Recursively appends a requirement (and its children) to the Markdown export.
     *
     * @param md    shared builder for the full document
     * @param req   requirement being rendered
     * @param level heading depth (e.g., 2 => ##)
     */
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

    /**
     * Ensures the requesting user is a member of the project before exposing requirement data.
     *
     * @param projectId identifier of the project being exported
     * @throws ExportException when the caller lacks access
     */
    private void checkAccess(UUID projectId) {
        User currentUser = authService.getCurrentUser();
        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, currentUser.getId())) {
            throw new ExportException("Access denied");
        }
    }

    /**
     * Escapes CSV fields by wrapping values that contain commas, quotes, or newlines in quotes and
     * doubling embedded quotes.
     *
     * @param value raw value to escape
     * @return safe CSV field value
     */
    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
