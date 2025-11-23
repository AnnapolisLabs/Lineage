package com.annapolislabs.lineage.service;

import com.annapolislabs.lineage.entity.TaskAssignment;
import com.annapolislabs.lineage.entity.User;
import com.annapolislabs.lineage.entity.UserStatus;
import com.annapolislabs.lineage.repository.TaskAssignmentRepository;
import com.annapolislabs.lineage.repository.UserRepository;
import com.annapolislabs.lineage.security.SecurityAuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskAssignmentServiceTest {

    @Mock
    private TaskAssignmentRepository taskAssignmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PermissionEvaluationService permissionEvaluationService;

    @Mock
    private EmailService emailService;

    @Mock
    private SecurityAuditService securityAuditService;

    @InjectMocks
    private TaskAssignmentService taskAssignmentService;

    private UUID userId;
    private UUID projectId;
    private User requester;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        projectId = UUID.randomUUID();

        requester = new User();
        requester.setId(userId);
        requester.setStatus(UserStatus.ACTIVE);
        requester.setEmail("test@lineage.com");

        lenient().when(userRepository.findById(userId)).thenReturn(Optional.of(requester));
        lenient().when(permissionEvaluationService.hasPermission(eq(userId), anyString(), any())).thenReturn(true);
    }

    @Test
    void searchTasks_returnsFilteredPage() {
        Pageable pageable = PageRequest.of(0, 20);
        TaskAssignment assignment = new TaskAssignment();
        assignment.setProjectId(projectId);
        assignment.setAssignedTo(userId);

        when(taskAssignmentRepository.findWithFilters(any(), any(), any(), any(), any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(assignment)));

        Page<TaskAssignment> result = taskAssignmentService.searchTasks(
                null,
                userId,
                projectId,
                TaskAssignment.TaskStatus.ASSIGNED,
                TaskAssignment.TaskPriority.MEDIUM,
                pageable,
                userId);

        assertEquals(1, result.getTotalElements());
        verify(taskAssignmentRepository).findWithFilters(eq(userId), isNull(), eq(projectId),
                eq(TaskAssignment.TaskStatus.ASSIGNED), eq(TaskAssignment.TaskPriority.MEDIUM), eq(pageable));
    }

    @Test
    void getOverdueTasks_delegatesToRepository() {
        TaskAssignment overdue = new TaskAssignment();
        overdue.setAssignedTo(userId);
        when(taskAssignmentRepository.findOverdueTasks(eq(userId), any(LocalDateTime.class)))
                .thenReturn(List.of(overdue));

        List<TaskAssignment> result = taskAssignmentService.getOverdueTasks(userId);

        assertEquals(1, result.size());
        verify(taskAssignmentRepository).findOverdueTasks(eq(userId), any(LocalDateTime.class));
    }

    @Test
    void getTasksDueSoon_usesWindowBounds() {
        when(taskAssignmentRepository.findTasksDueSoon(eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        List<TaskAssignment> result = taskAssignmentService.getTasksDueSoon(userId, 3);

        assertNotNull(result);
        verify(taskAssignmentRepository).findTasksDueSoon(eq(userId), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void getTaskStatistics_requiresPermissionAndReturnsCounts() {
        when(taskAssignmentRepository.getTaskCountByStatus()).thenReturn(List.of());
        when(taskAssignmentRepository.getTaskCountByPriority()).thenReturn(List.of());
        when(taskAssignmentRepository.getActiveTaskCountByUser()).thenReturn(List.of());

        Map<String, Object> stats = taskAssignmentService.getTaskStatistics(projectId, userId);

        assertTrue(stats.containsKey("by_status"));
        verify(taskAssignmentRepository).getTaskCountByStatus();
        verify(taskAssignmentRepository).getTaskCountByPriority();
        verify(taskAssignmentRepository).getActiveTaskCountByUser();
    }
}
