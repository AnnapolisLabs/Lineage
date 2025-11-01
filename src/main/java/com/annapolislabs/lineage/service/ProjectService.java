package com.annapolislabs.lineage.service;

import com.annapolislabs.lineage.dto.request.CreateProjectRequest;
import com.annapolislabs.lineage.dto.response.ProjectResponse;
import com.annapolislabs.lineage.entity.Project;
import com.annapolislabs.lineage.entity.ProjectMember;
import com.annapolislabs.lineage.entity.ProjectRole;
import com.annapolislabs.lineage.entity.User;
import com.annapolislabs.lineage.repository.ProjectMemberRepository;
import com.annapolislabs.lineage.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final AuthService authService;

    @Autowired
    public ProjectService(ProjectRepository projectRepository,
                         ProjectMemberRepository projectMemberRepository,
                         AuthService authService) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.authService = authService;
    }

    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request) {
        User currentUser = authService.getCurrentUser();

        if (projectRepository.existsByProjectKey(request.getProjectKey())) {
            throw new RuntimeException("Project key already exists: " + request.getProjectKey());
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

    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllProjects() {
        User currentUser = authService.getCurrentUser();
        return projectRepository.findAllByUserId(currentUser.getId())
                .stream()
                .map(ProjectResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(UUID projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Check if user has access
        User currentUser = authService.getCurrentUser();
        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, currentUser.getId())) {
            throw new RuntimeException("Access denied");
        }

        return new ProjectResponse(project);
    }

    @Transactional
    public ProjectResponse updateProject(UUID projectId, CreateProjectRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Check if user has admin access
        User currentUser = authService.getCurrentUser();
        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, currentUser.getId())
                .orElseThrow(() -> new RuntimeException("Access denied"));

        if (member.getRole() != ProjectRole.ADMIN) {
            throw new RuntimeException("Admin access required");
        }

        project.setName(request.getName());
        project.setDescription(request.getDescription());
        // Don't allow changing project key after creation

        project = projectRepository.save(project);
        return new ProjectResponse(project);
    }

    @Transactional
    public void deleteProject(UUID projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Check if user has admin access
        User currentUser = authService.getCurrentUser();
        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, currentUser.getId())
                .orElseThrow(() -> new RuntimeException("Access denied"));

        if (member.getRole() != ProjectRole.ADMIN) {
            throw new RuntimeException("Admin access required");
        }

        projectRepository.delete(project);
    }
}
