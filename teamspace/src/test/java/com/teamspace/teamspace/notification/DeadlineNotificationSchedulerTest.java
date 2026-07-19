package com.teamspace.teamspace.notification;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.teamspace.teamspace.notification.enums.NotificationType;
import com.teamspace.teamspace.notification.service.DeadlineNotificationScheduler;
import com.teamspace.teamspace.notification.service.NotificationService;
import com.teamspace.teamspace.planning.enums.PlanningState;
import com.teamspace.teamspace.planning.enums.WeeklyPlanStatus;
import com.teamspace.teamspace.planning.repository.WeeklyPlanRepository;
import com.teamspace.teamspace.project.entity.Project;
import com.teamspace.teamspace.task.entity.Task;
import com.teamspace.teamspace.task.repository.TaskRepository;
import com.teamspace.teamspace.task.repository.TaskWatcherRepository;
import com.teamspace.teamspace.user.entity.User;
import com.teamspace.teamspace.workspace.repository.WorkspaceMemberRepository;

@ExtendWith(MockitoExtension.class)
class DeadlineNotificationSchedulerTest {
    @Mock TaskRepository tasks;
    @Mock TaskWatcherRepository watchers;
    @Mock WeeklyPlanRepository weeklyPlans;
    @Mock WorkspaceMemberRepository members;
    @Mock NotificationService notifications;

    private DeadlineNotificationScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new DeadlineNotificationScheduler(tasks, watchers, weeklyPlans, members, notifications);
    }

    @Test
    void sendsSubtaskDeadlineReminderToParentAssigneeWithoutChangingParent() {
        LocalDate today = LocalDate.now();
        User subtaskAssignee = User.builder().id(11L).fullName("Subtask owner").build();
        User parentAssignee = User.builder().id(12L).fullName("Parent owner").build();
        Project project = Project.builder().id(21L).name("Project").build();
        Task parent = Task.builder().id(31L).title("Parent task").assignedTo(parentAssignee).project(project).build();
        Task subtask = Task.builder().id(32L).title("Child task").assignedTo(subtaskAssignee)
                .parentTask(parent).project(project).build();

        when(tasks.findByDueDateAndPlanningState(today.plusDays(1), PlanningState.ACTIVE))
                .thenReturn(List.of(subtask));
        when(tasks.findByDueDateBeforeAndPlanningState(today, PlanningState.ACTIVE)).thenReturn(List.of());
        when(weeklyPlans.findByEndDateAndStatus(today.plusDays(1), WeeklyPlanStatus.ACTIVE)).thenReturn(List.of());
        when(watchers.findByTaskIdOrderByCreatedAtAsc(32L)).thenReturn(List.of());
        when(watchers.findByTaskIdOrderByCreatedAtAsc(31L)).thenReturn(List.of());

        scheduler.sendDeadlineNotifications();

        verify(notifications).createAndSendDetailed(
                eq(parentAssignee), eq("Subtask sắp đến hạn"), contains("Thuộc Parent task"),
                eq(NotificationType.TASK_DUE_SOON), eq(32L), eq(21L), eq(32L),
                eq("deadline:TASK_DUE_SOON:32:12:" + today));
    }
}
