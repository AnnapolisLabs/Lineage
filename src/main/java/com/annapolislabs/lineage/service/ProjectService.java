package com.annapolislabs.lineage.service;

import com.annapolislabs.lineage.common.ServiceConstants;
import com.annapolislabs.lineage.dto.request.CreateProjectRequest;
import com.annapolislabs.lineage.dto.response.ProjectResponse;
import com.annapolislabs.lineage.entity.Project;
import com.annapolislabs.lineage.entity.ProjectMember;
import com.annapolislabs.lineage.entity.ProjectRole;
import com.annapolislabs.lineage.entity.Requirement;
import com.annapolislabs.lineage.entity.User;
import com.annapolislabs.lineage.exception.AccessDeniedException;
import com.annapolislabs.lineage.exception.DuplicateKeyException;
import com.annapolislabs.lineage.exception.ResourceNotFoundException;
import com.annapolislabs.lineage.repository.ProjectMemberRepository;
import com.annapolislabs.lineage.repository.ProjectRepository;
import com.annapolislabs.lineage.repository.RequirementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Handles project lifecycle operations including creation, retrieval, updates, and deletion while
 * enforcing membership-based authorization and audit-friendly history creation.
 */
@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final AuthService authService;
    private final RequirementRepository requirementRepository;

    @Autowired
    public ProjectService(ProjectRepository projectRepository,
                         ProjectMemberRepository projectMemberRepository,
                         AuthService authService,
                         RequirementRepository requirementRepository) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.authService = authService;
        this.requirementRepository = requirementRepository;
    }

    /**
     * Creates a new project with the current user set as the initial admin member. The method
     * enforces uniqueness of the project key and persists both the project entity and the creator's
     * membership within the same transaction.
     *
     * @param request payload containing project metadata and desired key
     * @return DTO representation of the persisted project
     * @throws DuplicateKeyException when the requested project key already exists
     */
    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request) {
        User currentUser = authService.getCurrentUser();

        if (projectRepository.existsByProjectKey(request.getProjectKey())) {
            throw new DuplicateKeyException("Project key already exists: " + request.getProjectKey());
        }

        Project project = new Project(
                request.getName(),
                request.getDescription(),
                request.getProjectKey(),
                currentUser
        );

        project = projectRepository.save(project);

        // Add creator as project admin
        ProjectMember member = new ProjectMember(project, currentUser, ProjectRole.ADMIN);
        projectMemberRepository.save(member);

        return new ProjectResponse(project);
    }

    /**
     * Retrieves every project the current user belongs to, mapping each entity into a lightweight
     * {@link ProjectResponse}. Uses a read-only transaction because the method only performs queries.
     *
     * @return list of projects ordered according to repository defaults
     */
    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllProjects() {
        User currentUser = authService.getCurrentUser();
        return projectRepository.findAllByUserId(currentUser.getId())
                .stream()
                .map(ProjectResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(UUID projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(ServiceConstants.PROJECT_NOT_FOUND));

        // Check if user has access
        User currentUser = authService.getCurrentUser();
        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, currentUser.getId())) {
            throw new AccessDeniedException();
        }

        return new ProjectResponse(project);
    }

    /**
     * Updates mutable project fields (name, description, level prefixes) when the caller is a project
     * admin. The project key remains immutable after creation to prevent identifier churn.
     *
     * @param projectId identifier of the project being updated
     * @param request   payload with updated values
     * @return latest project representation
     * @throws ResourceNotFoundException when the project cannot be located
     * @throws AccessDeniedException     when the caller is not an admin member of the project
     */
    @Transactional
    public ProjectResponse updateProject(UUID projectId, CreateProjectRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(ServiceConstants.PROJECT_NOT_FOUND));

        // Check if user has admin access
        User currentUser = authService.getCurrentUser();
        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, currentUser.getId())
                .orElseThrow(() -> new AccessDeniedException("Access denied"));

        if (member.getRole() != ProjectRole.ADMIN) {
            throw new AccessDeniedException("Admin access required");
        }

        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setLevelPrefixes(request.getLevelPrefixes());
        // Don't allow changing project key after creation

        project = projectRepository.save(project);
        return new ProjectResponse(project);
    }

    @Transactional
    public void deleteProject(UUID projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(ServiceConstants.PROJECT_NOT_FOUND));

        // Check if user has admin access
        User currentUser = authService.getCurrentUser();
        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, currentUser.getId())
                .orElseThrow(() -> new AccessDeniedException("Access denied"));

        if (member.getRole() != ProjectRole.ADMIN) {
            throw new AccessDeniedException("Admin access required");
        }

        // Delete all requirements associated with this project first
        List<Requirement> requirements = requirementRepository.findByProjectId(projectId);
        requirementRepository.deleteAll(requirements);

        // Delete all project members
        List<ProjectMember> members = projectMemberRepository.findByProjectId(projectId);
        projectMemberRepository.deleteAll(members);

        // Finally delete the project
        projectRepository.delete(project);
    }
}
