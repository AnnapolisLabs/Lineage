package com.annapolislabs.lineage.repository;

import com.annapolislabs.lineage.entity.TaskAssignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for TaskAssignment entities
 */
@Repository
public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, UUID> {

    /**
     * Find tasks assigned to a specific user
     */
    List<TaskAssignment> findByAssignedToAndStatusIn(UUID assignedTo, List<TaskAssignment.TaskStatus> statuses);

    /**
     * Find active tasks for a specific user
     */
    @Query("SELECT ta FROM TaskAssignment ta WHERE ta.assignedTo = :assignedTo AND ta.status NOT IN ('completed', 'cancelled')")
    List<TaskAssignment> findByAssignedToAndActiveTrue(@Param("assignedTo") UUID assignedTo);
    
    /**
     * Find tasks assigned by a specific user
     */
    List<TaskAssignment> findByAssignedByAndStatusIn(UUID assignedBy, List<TaskAssignment.TaskStatus> statuses);
    
    /**
     * Find tasks by project ID
     */
    List<TaskAssignment> findByProjectIdAndStatusIn(UUID projectId, List<TaskAssignment.TaskStatus> statuses);
    
    /**
     * Find tasks by requirement ID
     */
    List<TaskAssignment> findByRequirementId(UUID requirementId);
    
    /**
     * Find overdue tasks for a specific user
     */
    @Query("SELECT ta FROM TaskAssignment ta WHERE ta.assignedTo = :assignedTo AND ta.dueDate < :now AND ta.status NOT IN ('completed', 'cancelled')")
    List<TaskAssignment> findOverdueTasks(@Param("assignedTo") UUID assignedTo, @Param("now") LocalDateTime now);
    
    /**
     * Find tasks due soon (within specified window) for a specific user
     */
    @Query("SELECT ta FROM TaskAssignment ta WHERE ta.assignedTo = :assignedTo AND ta.dueDate BETWEEN :now AND :dueSoon AND ta.status NOT IN ('completed', 'cancelled')")
    List<TaskAssignment> findTasksDueSoon(@Param("assignedTo") UUID assignedTo,
                                          @Param("now") LocalDateTime now,
                                          @Param("dueSoon") LocalDateTime dueSoon);
    
    /**
     * Find tasks by status
     */
    List<TaskAssignment> findByStatus(TaskAssignment.TaskStatus status);
    
    /**
     * Find tasks by priority
     */
    List<TaskAssignment> findByPriority(TaskAssignment.TaskPriority priority);
    
    /**
     * Find tasks with filtering
     */
    @Query("SELECT ta FROM TaskAssignment ta WHERE " +
           "(:assignedTo IS NULL OR ta.assignedTo = :assignedTo) AND " +
           "(:assignedBy IS NULL OR ta.assignedBy = :assignedBy) AND " +
           "(:projectId IS NULL OR ta.projectId = :projectId) AND " +
           "(:status IS NULL OR ta.status = :status) AND " +
           "(:priority IS NULL OR ta.priority = :priority)")
    Page<TaskAssignment> findWithFilters(@Param("assignedTo") UUID assignedTo,
                                        @Param("assignedBy") UUID assignedBy,
                                        @Param("projectId") UUID projectId,
                                        @Param("status") TaskAssignment.TaskStatus status,
                                        @Param("priority") TaskAssignment.TaskPriority priority,
                                        Pageable pageable);
    
    /**
     * Search tasks by title or description
     */
    @Query("SELECT ta FROM TaskAssignment ta WHERE " +
           "LOWER(ta.taskTitle) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(ta.taskDescription) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<TaskAssignment> findBySearchTerm(@Param("search") String search, Pageable pageable);
    
    /**
     * Find high priority tasks
     */
    @Query("SELECT ta FROM TaskAssignment ta WHERE ta.priority IN ('high', 'critical') AND ta.status NOT IN ('completed', 'cancelled')")
    List<TaskAssignment> findHighPriorityTasks();
    
    /**
     * Find tasks without assignee
     */
    @Query("SELECT ta FROM TaskAssignment ta WHERE ta.assignedTo IS NULL AND ta.status = 'assigned'")
    List<TaskAssignment> findUnassignedTasks();
    
    /**
     * Get task statistics
     */
    @Query("SELECT ta.status, COUNT(ta) FROM TaskAssignment ta GROUP BY ta.status")
    List<Object[]> getTaskCountByStatus();
    
    @Query("SELECT ta.priority, COUNT(ta) FROM TaskAssignment ta GROUP BY ta.priority")
    List<Object[]> getTaskCountByPriority();
    
    @Query("SELECT ta.assignedTo, COUNT(ta) FROM TaskAssignment ta WHERE ta.status NOT IN ('completed', 'cancelled') GROUP BY ta.assignedTo")
    List<Object[]> getActiveTaskCountByUser();
    
    /**
     * Find tasks created after a date
     */
    @Query("SELECT ta FROM TaskAssignment ta WHERE ta.createdAt >= :startDate ORDER BY ta.createdAt DESC")
    List<TaskAssignment> findTasksCreatedAfter(@Param("startDate") LocalDateTime startDate, Pageable pageable);
    
    /**
     * Find tasks by tags
     */
    @Query("SELECT ta FROM TaskAssignment ta JOIN ta.tags tag WHERE tag = :tag AND ta.status NOT IN ('completed', 'cancelled')")
    List<TaskAssignment> findByTag(@Param("tag") String tag);
    
    /**
     * Find tasks with blockers
     */
    @Query("SELECT ta FROM TaskAssignment ta WHERE SIZE(ta.blockers) > 0")
    List<TaskAssignment> findTasksWithBlockers();
    
    /**
     * Find completed tasks within date range
     */
    @Query("SELECT ta FROM TaskAssignment ta WHERE ta.status = 'completed' AND ta.completedAt BETWEEN :startDate AND :endDate")
    List<TaskAssignment> findCompletedTasksInRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}