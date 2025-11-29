package com.annapolislabs.lineage.controller;

import com.annapolislabs.lineage.service.TaskAssignmentService;
import com.annapolislabs.lineage.entity.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST controller for task assignment and management operations
 * Provides endpoints for task CRUD operations, assignment tracking, and completion workflows
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Task Management", description = "Task assignment, tracking, and completion APIs")
public class TaskAssignmentController {

    private final TaskAssignmentService taskAssignmentService;

    /**
     * Get all tasks accessible to the current user
     */
    @GetMapping
    @Operation(
        summary = "Get tasks", 
        description = "Retrieve tasks accessible to the current user with optional filtering"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Page<TaskAssignment>> getTasks(
            @Parameter(description = "Filter by assigned user ID")
            @RequestParam(required = false) UUID assignedTo,
            
            @Parameter(description = "Filter by project ID")
            @RequestParam(required = false) UUID projectId,
            
            @Parameter(description = "Filter by task status")
            @RequestParam(required = false) String status,
            
            @Parameter(description = "Filter by priority")
            @RequestParam(required = false) String priority,
            
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {
        
        UUID currentUserId = getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        
        TaskAssignment.TaskStatus taskStatus = status != null ? 
                TaskAssignment.TaskStatus.valueOf(status.toUpperCase()) : null;
        TaskAssignment.TaskPriority taskPriority = priority != null ?
                TaskAssignment.TaskPriority.valueOf(priority.toUpperCase()) : null;
        
        Page<TaskAssignment> tasks = taskAssignmentService.searchTasks(
                null,
                assignedTo,
                projectId,
                taskStatus,
                taskPriority,
                pageable,
                currentUserId);
        
        return ResponseEntity.ok(tasks);
    }

    /**
     * Create a new task assignment
     */
    @PostMapping
    @Operation(
        summary = "Create task", 
        description = "Create a new task assignment"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Task created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
    })
    public ResponseEntity<TaskAssignment> createTask(
            @Parameter(description = "Task creation request", required = true)
            @RequestBody Map<String, Object> request) {
        
        UUID currentUserId = getCurrentUserId();
        
        String taskTitle = (String) request.get("task_title");
        String taskDescription = (String) request.get("task_description");
        UUID assignedTo = UUID.fromString(request.get("assigned_to").toString());
        UUID projectId = UUID.fromString(request.get("project_id").toString());
        UUID requirementId = request.get("requirement_id") != null ? 
                UUID.fromString(request.get("requirement_id").toString()) : null;
        String priorityName = (String) request.get("priority");
        String dueDateStr = (String) request.get("due_date");
        
        TaskAssignment.TaskPriority priority = priorityName != null ? 
                TaskAssignment.TaskPriority.valueOf(priorityName.toUpperCase()) : 
                TaskAssignment.TaskPriority.MEDIUM;
        
        LocalDateTime dueDate = dueDateStr != null ? 
                LocalDateTime.parse(dueDateStr) : null;
        
        TaskAssignment task = taskAssignmentService.createTask(
                taskTitle, taskDescription, assignedTo, projectId, requirementId,
                priority, dueDate, currentUserId);
        
        log.info("Task created successfully: {}", task.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(task);
    }

    /**
     * Get task by ID
     */
    @GetMapping("/{taskId}")
    @Operation(
        summary = "Get task details", 
        description = "Retrieve detailed information about a specific task"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Task details retrieved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<TaskAssignment> getTask(
            @Parameter(description = "Task ID", required = true)
            @PathVariable UUID taskId) {
        
        UUID currentUserId = getCurrentUserId();
        TaskAssignment task = taskAssignmentService.getTaskById(taskId, currentUserId);
        
        return ResponseEntity.ok(task);
    }

    /**
     * Update task information
     */
    @PutMapping("/{taskId}")
    @Operation(
        summary = "Update task", 
        description = "Update task information and settings"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Task updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<TaskAssignment> updateTask(
            @Parameter(description = "Task ID", required = true)
            @PathVariable UUID taskId,
            
            @Parameter(description = "Task update request", required = true)
            @RequestBody Map<String, Object> request) {
        
        UUID currentUserId = getCurrentUserId();
        
        String taskTitle = (String) request.get("task_title");
        String taskDescription = (String) request.get("task_description");
        String priorityName = (String) request.get("priority");
        String dueDateStr = (String) request.get("due_date");
        
        TaskAssignment.TaskPriority priority = priorityName != null ? 
                TaskAssignment.TaskPriority.valueOf(priorityName.toUpperCase()) : null;
        
        LocalDateTime dueDate = dueDateStr != null ? 
                LocalDateTime.parse(dueDateStr) : null;
        
        TaskAssignment task = taskAssignmentService.updateTask(
                taskId, taskTitle, taskDescription, priority, dueDate, currentUserId);
        
        log.info("Task updated successfully: {}", taskId);
        return ResponseEntity.ok(task);
    }

    /**
     * Start work on a task
     */
    @PostMapping("/{taskId}/start")
    @Operation(
        summary = "Start task", 
        description = "Mark a task as in progress"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Task started successfully"),
        @ApiResponse(responseCode = "400", description = "Task cannot be started"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<Map<String, String>> startTask(
            @Parameter(description = "Task ID", required = true)
            @PathVariable UUID taskId) {
        
        UUID currentUserId = getCurrentUserId();
        taskAssignmentService.startTask(taskId, currentUserId);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Task started successfully");
        
        log.info("Task started successfully: {}", taskId);
        return ResponseEntity.ok(response);
    }

    /**
     * Complete a task
     */
    @PostMapping("/{taskId}/complete")
    @Operation(
        summary = "Complete task", 
        description = "Mark a task as completed"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Task completed successfully"),
        @ApiResponse(responseCode = "400", description = "Task cannot be completed"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<Map<String, String>> completeTask(
            @Parameter(description = "Task ID", required = true)
            @PathVariable UUID taskId,
            
            @Parameter(description = "Task completion request")
            @RequestBody Map<String, Object> request) {
        
        UUID currentUserId = getCurrentUserId();
        
        String completionNotes = (String) request.get("completion_notes");
        
        taskAssignmentService.completeTask(taskId, completionNotes, currentUserId);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Task completed successfully");
        
        log.info("Task completed successfully: {}", taskId);
        return ResponseEntity.ok(response);
    }

    /**
     * Cancel a task
     */
    @PostMapping("/{taskId}/cancel")
    @Operation(
        summary = "Cancel task", 
        description = "Cancel a task assignment"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Task cancelled successfully"),
        @ApiResponse(responseCode = "400", description = "Task cannot be cancelled"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<Map<String, String>> cancelTask(
            @Parameter(description = "Task ID", required = true)
            @PathVariable UUID taskId,
            
            @Parameter(description = "Task cancellation request", required = true)
            @RequestBody Map<String, Object> request) {
        
        UUID currentUserId = getCurrentUserId();
        
        String reason = (String) request.get("reason");
        
        taskAssignmentService.cancelTask(taskId, reason, currentUserId);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Task cancelled successfully");
        
        log.info("Task cancelled successfully: {}", taskId);
        return ResponseEntity.ok(response);
    }

    /**
     * Reassign task to another user
     */
    @PostMapping("/{taskId}/reassign")
    @Operation(
        summary = "Reassign task", 
        description = "Reassign a task to another user"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Task reassigned successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid user or task"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<Map<String, String>> reassignTask(
            @Parameter(description = "Task ID", required = true)
            @PathVariable UUID taskId,
            
            @Parameter(description = "Task reassignment request", required = true)
            @RequestBody Map<String, Object> request) {
        
        UUID currentUserId = getCurrentUserId();
        
        UUID newAssigneeId = UUID.fromString(request.get("assigned_to").toString());
        
        taskAssignmentService.reassignTask(taskId, newAssigneeId, currentUserId);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Task reassigned successfully");
        
        log.info("Task reassigned successfully: {} to {}", taskId, newAssigneeId);
        return ResponseEntity.ok(response);
    }

    /**
     * Add tag to task
     */
    @PostMapping("/{taskId}/tags")
    @Operation(
        summary = "Add tag to task", 
        description = "Add a tag to a task"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Tag added successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid tag"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<Map<String, String>> addTagToTask(
            @Parameter(description = "Task ID", required = true)
            @PathVariable UUID taskId,
            
            @Parameter(description = "Tag request", required = true)
            @RequestBody Map<String, Object> request) {
        
        UUID currentUserId = getCurrentUserId();
        
        String tag = (String) request.get("tag");
        
        taskAssignmentService.addTagToTask(taskId, tag, currentUserId);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Tag added successfully");
        response.put("tag", tag);
        
        log.info("Tag '{}' added to task {}", tag, taskId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Add blocker to task
     */
    @PostMapping("/{taskId}/blockers")
    @Operation(
        summary = "Add blocker to task", 
        description = "Add a blocker to a task"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Blocker added successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid blocker"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<Map<String, String>> addBlockerToTask(
            @Parameter(description = "Task ID", required = true)
            @PathVariable UUID taskId,
            
            @Parameter(description = "Blocker request", required = true)
            @RequestBody Map<String, Object> request) {
        
        UUID currentUserId = getCurrentUserId();
        
        String blocker = (String) request.get("blocker");
        
        taskAssignmentService.addBlockerToTask(taskId, blocker, currentUserId);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Blocker added successfully");
        response.put("blocker", blocker);
        
        log.info("Blocker '{}' added to task {}", blocker, taskId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Search tasks
     */
    @GetMapping("/search")
    @Operation(
        summary = "Search tasks", 
        description = "Search tasks by title, description, or other criteria"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search results retrieved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Page<TaskAssignment>> searchTasks(
            @Parameter(description = "Search term")
            @RequestParam(required = false) String search,
            
            @Parameter(description = "Filter by assigned user ID")
            @RequestParam(required = false) UUID assignedTo,
            
            @Parameter(description = "Filter by project ID")
            @RequestParam(required = false) UUID projectId,
            
            @Parameter(description = "Filter by task status")
            @RequestParam(required = false) String status,
            
            @Parameter(description = "Filter by priority")
            @RequestParam(required = false) String priority,
            
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {
        
        UUID currentUserId = getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        
        TaskAssignment.TaskStatus taskStatus = status != null ? 
                TaskAssignment.TaskStatus.valueOf(status.toUpperCase()) : null;
        TaskAssignment.TaskPriority taskPriority = priority != null ? 
                TaskAssignment.TaskPriority.valueOf(priority.toUpperCase()) : null;
        
        Page<TaskAssignment> tasks = taskAssignmentService.searchTasks(
                search, assignedTo, projectId, taskStatus, taskPriority, pageable, currentUserId);
        
        return ResponseEntity.ok(tasks);
    }

    /**
     * Get overdue tasks
     */
    @GetMapping("/overdue")
    @Operation(
        summary = "Get overdue tasks", 
        description = "Retrieve tasks that are past their due date"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Overdue tasks retrieved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<TaskAssignment>> getOverdueTasks() {
        UUID currentUserId = getCurrentUserId();
        List<TaskAssignment> overdueTasks = taskAssignmentService.getOverdueTasks(currentUserId);
        
        return ResponseEntity.ok(overdueTasks);
    }

    /**
     * Get tasks due soon
     */
    @GetMapping("/due-soon")
    @Operation(
        summary = "Get tasks due soon", 
        description = "Retrieve tasks that are due within a specified time period"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tasks due soon retrieved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<TaskAssignment>> getTasksDueSoon(
            @Parameter(description = "Days threshold (default: 7)")
            @RequestParam(defaultValue = "7") int daysThreshold) {
        
        UUID currentUserId = getCurrentUserId();
        List<TaskAssignment> dueSoonTasks = taskAssignmentService.getTasksDueSoon(currentUserId, daysThreshold);
        
        return ResponseEntity.ok(dueSoonTasks);
    }

    /**
     * Get current user ID from authentication context
     */
    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("User not authenticated");
        }
        
        Object principal = authentication.getPrincipal();
        if (principal instanceof String) {
            try {
                return UUID.fromString((String) principal);
            } catch (IllegalArgumentException e) {
                throw new SecurityException("Invalid user ID in authentication context");
            }
        }
        
        throw new SecurityException("Unable to extract user ID from authentication context");
    }
}