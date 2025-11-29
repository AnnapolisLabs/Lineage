package com.annapolislabs.lineage.service;

import com.annapolislabs.lineage.entity.*;
import com.annapolislabs.lineage.repository.*;
import com.annapolislabs.lineage.security.SecurityAuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Simplified Task Assignment Service
 * Handles basic task assignment operations for the collaboration system
 */
@Slf4j
@Service
public class TaskAssignmentService {

    @Autowired
    private TaskAssignmentRepository taskAssignmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RequirementRepository requirementRepository;

    @Autowired
    private PermissionEvaluationService permissionEvaluationService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private SecurityAuditService securityAuditService;

    /**
     * Create a new task assignment
     */
    @Transactional
    public TaskAssignment createTask(String taskTitle, String taskDescription,
                                    UUID assignedTo, UUID projectId, UUID requirementId,
                                    TaskAssignment.TaskPriority priority, LocalDateTime dueDate,
                                    UUID requestingUserId) {
        if (!StringUtils.hasText(taskTitle)) {
            throw new IllegalArgumentException("Task title is required");
        }
        if (assignedTo == null) {
            throw new IllegalArgumentException("Assignee user ID is required");
        }
        if (requestingUserId == null) {
            throw new IllegalArgumentException("Requesting user ID is required");
        }

        UUID assignedBy = requestingUserId;
        log.info("Creating task '{}' assigned to {} by {}", taskTitle, assignedTo, assignedBy);

        // Check permissions
        if (!permissionEvaluationService.hasPermission(assignedBy, "task.assign", projectId)) {
            throw new SecurityException("User does not have permission to assign tasks");
        }

        // Validate users exist
        User assignee = userRepository.findById(assignedTo)
                .orElseThrow(() -> new IllegalArgumentException("Assignee user not found"));
        
        if (assignee.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Cannot assign tasks to inactive user");
        }

        userRepository.findById(assignedBy)
                .orElseThrow(() -> new IllegalArgumentException("Assigner user not found"));

        // Create task
        TaskAssignment task = new TaskAssignment();
        task.setTaskTitle(taskTitle);
        task.setTaskDescription(taskDescription);
        task.setAssignedBy(assignedBy);
        task.setAssignedTo(assignedTo);
        task.setProjectId(projectId);
        task.setRequirementId(requirementId);
        task.setStatus(TaskAssignment.TaskStatus.ASSIGNED);
        task.setPriority(priority != null ? priority : TaskAssignment.TaskPriority.MEDIUM);
        task.setDueDate(dueDate);

        task = taskAssignmentRepository.save(task);

        // Send notification email
        try {
            emailService.sendTaskAssignmentNotification(
                assignee.getEmail(),
                taskTitle,
                taskDescription,
                projectId.toString()
            );
        } catch (Exception e) {
            log.warn("Failed to send task assignment email", e);
        }

        // Audit log
        securityAuditService.logEvent("TASK_CREATED", assignedBy, "TASK", task.getId(),
            Map.of("task_title", taskTitle, "assigned_to", assignedTo, "project_id", projectId));

        log.info("Task '{}' created successfully with ID {}", taskTitle, task.getId());
        return task;
    }

    /**
     * Get task by ID
     */
    public TaskAssignment getTaskById(UUID taskId, UUID requestingUserId) {
        log.debug("Getting task {} for user {}", taskId, requestingUserId);

        TaskAssignment task = taskAssignmentRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        // Check permissions
        if (!task.getAssignedTo().equals(requestingUserId) && 
            !task.getAssignedBy().equals(requestingUserId) &&
            !permissionEvaluationService.hasPermission(requestingUserId, "task.read", task.getProjectId())) {
            throw new SecurityException("User does not have permission to view this task");
        }

        return task;
    }

    /**
     * Complete a task
     */
    @Transactional
    public TaskAssignment completeTask(UUID taskId, String completionNotes, UUID requestingUserId) {
        return updateTaskStatus(taskId, TaskAssignment.TaskStatus.COMPLETED, requestingUserId, completionNotes);
    }

    /**
     * Update task status
     */
    @Transactional
    public TaskAssignment updateTaskStatus(UUID taskId, TaskAssignment.TaskStatus newStatus, 
                                         UUID requestingUserId, String completionNotes) {
        log.info("Updating task {} status to {} by user {}", taskId, newStatus, requestingUserId);

        TaskAssignment task = taskAssignmentRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        // Check permissions
        if (!task.getAssignedTo().equals(requestingUserId) &&
            !task.getAssignedBy().equals(requestingUserId) &&
            !permissionEvaluationService.hasPermission(requestingUserId, "task.manage", task.getProjectId())) {
            throw new SecurityException("User does not have permission to update this task");
        }

        TaskAssignment.TaskStatus oldStatus = task.getStatus();
        task.setStatus(newStatus);

        if (newStatus == TaskAssignment.TaskStatus.COMPLETED) {
            task.setCompletedAt(LocalDateTime.now());
            if (StringUtils.hasText(completionNotes)) {
                task.setCompletionNotes(completionNotes);
            }
        }

        task = taskAssignmentRepository.save(task);

        // Audit log
        securityAuditService.logEvent("TASK_STATUS_UPDATED", requestingUserId, "TASK", taskId,
            Map.of("old_status", oldStatus.name(), "new_status", newStatus.name()));

        log.info("Task {} status updated from {} to {}", taskId, oldStatus, newStatus);
        return task;
    }

    /**
     * Get tasks assigned to user
     */
    public List<TaskAssignment> getTasksForUser(UUID userId, UUID requestingUserId) {
        log.debug("Getting tasks for user {} requested by {}", userId, requestingUserId);

        // Users can only view their own tasks unless they have manage permissions
        if (!userId.equals(requestingUserId) && 
            !permissionEvaluationService.hasPermission(requestingUserId, "task.manage", null)) {
            throw new SecurityException("User does not have permission to view other users' tasks");
        }

        return taskAssignmentRepository.findByAssignedToAndActiveTrue(userId);
    }

    /**
     * Get overdue tasks
     */
    public List<TaskAssignment> getOverdueTasks(UUID userId) {
        log.debug("Getting overdue tasks for user {}", userId);
        LocalDateTime now = LocalDateTime.now();
        return taskAssignmentRepository.findOverdueTasks(userId, now);
    }

    /**
     * Get tasks due soon
     */
    public List<TaskAssignment> getTasksDueSoon(UUID userId, int daysThreshold) {
        log.debug("Getting tasks due soon for user {} within {} days", userId, daysThreshold);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dueSoon = now.plusDays(daysThreshold);
        return taskAssignmentRepository.findTasksDueSoon(userId, now, dueSoon);
    }

    /**
     * Search tasks with multi-criteria filters
     */
    public Page<TaskAssignment> searchTasks(String search,
                                            UUID assignedTo,
                                            UUID projectId,
                                            TaskAssignment.TaskStatus status,
                                            TaskAssignment.TaskPriority priority,
                                            Pageable pageable,
                                            UUID requestingUserId) {
        log.debug("Searching tasks for user {} with filters search='{}', assignedTo={}, projectId={}, status={}",
                requestingUserId, search, assignedTo, projectId, status);

        // Permission: user can query others only with manage rights
        if (assignedTo != null && !assignedTo.equals(requestingUserId) &&
                !permissionEvaluationService.hasPermission(requestingUserId, "task.manage", projectId)) {
            throw new SecurityException("User does not have permission to view other users' tasks");
        }

        // If search text provided, delegate to text search first
        if (StringUtils.hasText(search)) {
            Page<TaskAssignment> results = taskAssignmentRepository.findBySearchTerm(search, pageable);
            return filterByAuthorization(results, requestingUserId);
        }

        return taskAssignmentRepository.findWithFilters(
                assignedTo != null ? assignedTo : requestingUserId,
                null,
                projectId,
                status,
                priority,
                pageable);
    }

    /**
     * Get tasks for specific project enforcing permissions
     */
    public List<TaskAssignment> getTasksByProject(UUID projectId, UUID requestingUserId) {
        log.debug("Getting tasks for project {} by user {}", projectId, requestingUserId);
        if (!permissionEvaluationService.hasPermission(requestingUserId, "task.read", projectId)) {
            throw new SecurityException("User does not have permission to view tasks for this project");
        }

        return taskAssignmentRepository.findByProjectIdAndStatusIn(projectId,
                Arrays.asList(TaskAssignment.TaskStatus.values()));
    }

    /**
     * Task statistics aggregated by status/priority
     */
    public Map<String, Object> getTaskStatistics(UUID projectId, UUID requestingUserId) {
        log.debug("Getting task statistics for project {} by user {}", projectId, requestingUserId);
        if (!permissionEvaluationService.hasPermission(requestingUserId, "task.manage", projectId)) {
            throw new SecurityException("User does not have permission to view task statistics");
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("by_status", taskAssignmentRepository.getTaskCountByStatus());
        stats.put("by_priority", taskAssignmentRepository.getTaskCountByPriority());
        stats.put("active_by_user", taskAssignmentRepository.getActiveTaskCountByUser());
        return stats;
    }

    private Page<TaskAssignment> filterByAuthorization(Page<TaskAssignment> tasks, UUID requestingUserId) {
        List<TaskAssignment> filtered = tasks.stream()
                .filter(task -> task.getAssignedTo().equals(requestingUserId) ||
                        task.getAssignedBy().equals(requestingUserId) ||
                        permissionEvaluationService.hasPermission(requestingUserId, "task.read", task.getProjectId()))
                .collect(Collectors.toList());
        return new org.springframework.data.domain.PageImpl<>(filtered, tasks.getPageable(), filtered.size());
    }

    /**
     * Start task (mark as in progress)
     */
    @Transactional
    public TaskAssignment startTask(UUID taskId, UUID requestingUserId) {
        return updateTaskStatus(taskId, TaskAssignment.TaskStatus.IN_PROGRESS, requestingUserId, null);
    }

    /**
     * Cancel task
     */
    @Transactional
    public TaskAssignment cancelTask(UUID taskId, String reason, UUID requestingUserId) {
        return updateTaskStatus(taskId, TaskAssignment.TaskStatus.CANCELLED, requestingUserId, reason);
    }

    /**
     * Reassign task
     */
    @Transactional
    public TaskAssignment reassignTask(UUID taskId, UUID newAssigneeId, UUID requestingUserId) {
        log.info("Reassigning task {} from current assignee to {} by user {}", taskId, newAssigneeId, requestingUserId);

        TaskAssignment task = taskAssignmentRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        // Check permissions
        if (!task.getAssignedBy().equals(requestingUserId) &&
            !permissionEvaluationService.hasPermission(requestingUserId, "task.manage", task.getProjectId())) {
            throw new SecurityException("User does not have permission to reassign this task");
        }

        // Validate new assignee
        User newAssignee = userRepository.findById(newAssigneeId)
                .orElseThrow(() -> new IllegalArgumentException("New assignee not found"));
        
        if (newAssignee.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Cannot reassign task to inactive user");
        }

        UUID oldAssignee = task.getAssignedTo();
        task.setAssignedTo(newAssigneeId);

        task = taskAssignmentRepository.save(task);

        // Audit log
        securityAuditService.logEvent("TASK_REASSIGNED", requestingUserId, "TASK", taskId,
            Map.of("old_assignee", oldAssignee, "new_assignee", newAssigneeId));

        log.info("Task {} reassigned from {} to {}", taskId, oldAssignee, newAssigneeId);
        return task;
    }

    /**
     * Add tag to task
     */
    @Transactional
    public TaskAssignment addTagToTask(UUID taskId, String tag, UUID requestingUserId) {
        TaskAssignment task = taskAssignmentRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        // Check permissions
        if (!task.getAssignedTo().equals(requestingUserId) &&
            !task.getAssignedBy().equals(requestingUserId)) {
            throw new SecurityException("User does not have permission to modify this task");
        }

        List<String> tags = task.getTags();
        if (tags == null) {
            tags = new ArrayList<>();
        }
        if (!tags.contains(tag)) {
            tags.add(tag);
        }
        task.setTags(tags);

        task = taskAssignmentRepository.save(task);

        log.info("Tag '{}' added to task {}", tag, taskId);
        return task;
    }

    /**
     * Add blocker to task
     */
    @Transactional
    public TaskAssignment addBlockerToTask(UUID taskId, String blocker, UUID requestingUserId) {
        TaskAssignment task = taskAssignmentRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        // Check permissions
        if (!task.getAssignedTo().equals(requestingUserId) &&
            !task.getAssignedBy().equals(requestingUserId) &&
            !permissionEvaluationService.hasPermission(requestingUserId, "task.manage", task.getProjectId())) {
            throw new SecurityException("User does not have permission to modify this task");
        }

        List<String> blockers = task.getBlockers();
        if (blockers == null) {
            blockers = new ArrayList<>();
        }
        if (!blockers.contains(blocker)) {
            blockers.add(blocker);
        }
        task.setBlockers(blockers);

        task = taskAssignmentRepository.save(task);

        log.info("Blocker '{}' added to task {}", blocker, taskId);
        return task;
    }

    /**
     * Update task basic info
     */
    @Transactional
    public TaskAssignment updateTask(UUID taskId, String taskTitle, String taskDescription,
                                   TaskAssignment.TaskPriority priority, LocalDateTime dueDate, 
                                   UUID requestingUserId) {
        log.info("Updating task {} by user {}", taskId, requestingUserId);

        TaskAssignment task = taskAssignmentRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        // Check permissions
        if (!task.getAssignedBy().equals(requestingUserId) &&
            !permissionEvaluationService.hasPermission(requestingUserId, "task.manage", task.getProjectId())) {
            throw new SecurityException("User does not have permission to update this task");
        }

        if (StringUtils.hasText(taskTitle)) {
            task.setTaskTitle(taskTitle);
        }
        if (StringUtils.hasText(taskDescription)) {
            task.setTaskDescription(taskDescription);
        }
        if (priority != null) {
            task.setPriority(priority);
        }
        if (dueDate != null) {
            task.setDueDate(dueDate);
        }

        task = taskAssignmentRepository.save(task);

        log.info("Task {} updated successfully", taskId);
        return task;
    }
}