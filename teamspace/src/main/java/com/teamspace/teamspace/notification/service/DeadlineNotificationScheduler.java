package com.teamspace.teamspace.notification.service;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamspace.teamspace.notification.enums.NotificationType;
import com.teamspace.teamspace.planning.entity.WeeklyPlan;
import com.teamspace.teamspace.planning.enums.PlanningState;
import com.teamspace.teamspace.planning.enums.WeeklyPlanStatus;
import com.teamspace.teamspace.planning.repository.WeeklyPlanRepository;
import com.teamspace.teamspace.task.entity.Task;
import com.teamspace.teamspace.task.repository.TaskRepository;
import com.teamspace.teamspace.task.repository.TaskWatcherRepository;
import com.teamspace.teamspace.user.entity.User;
import com.teamspace.teamspace.workspace.entity.WorkspaceMember;
import com.teamspace.teamspace.workspace.repository.WorkspaceMemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeadlineNotificationScheduler {
    private final TaskRepository tasks;
    private final TaskWatcherRepository watchers;
    private final WeeklyPlanRepository weeklyPlans;
    private final WorkspaceMemberRepository members;
    private final NotificationService notifications;

    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Bangkok")
    @Transactional
    public void sendDeadlineNotifications() {
        LocalDate today = LocalDate.now();
        tasks.findByDueDateAndPlanningState(today.plusDays(1), PlanningState.ACTIVE)
                .forEach(task -> notifyTask(task, NotificationType.TASK_DUE_SOON,
                        "Công việc sắp đến hạn", today));
        tasks.findByDueDateBeforeAndPlanningState(today, PlanningState.ACTIVE)
                .forEach(task -> notifyTask(task, NotificationType.TASK_OVERDUE,
                        "Công việc đã quá hạn", today));
        weeklyPlans.findByEndDateAndStatus(today.plusDays(1), WeeklyPlanStatus.ACTIVE)
                .forEach(plan -> notifyPlanEnding(plan, today));
    }

    private void notifyTask(Task task, NotificationType type, String title, LocalDate date) {
        Set<User> receivers = new LinkedHashSet<>();
        if (task.getAssignedTo() != null) receivers.add(task.getAssignedTo());
        watchers.findByTaskIdOrderByCreatedAtAsc(task.getId())
                .forEach(watcher -> receivers.add(watcher.getUser()));

        Task parentTask = task.getParentTask();
        if (parentTask != null) {
            if (parentTask.getAssignedTo() != null) receivers.add(parentTask.getAssignedTo());
            watchers.findByTaskIdOrderByCreatedAtAsc(parentTask.getId())
                    .forEach(watcher -> receivers.add(watcher.getUser()));
        }

        String reminderTitle = parentTask == null
                ? title
                : type == NotificationType.TASK_OVERDUE ? "Subtask đã quá hạn" : "Subtask sắp đến hạn";
        String reminderContent = parentTask == null
                ? task.getTitle()
                : task.getTitle() + " · Thuộc " + parentTask.getTitle();
        receivers.forEach(user -> notifications.createAndSendDetailed(
                user, reminderTitle, reminderContent, type, task.getId(), task.getProject().getId(), task.getId(),
                "deadline:" + type + ":" + task.getId() + ":" + user.getId() + ":" + date));
    }

    private void notifyPlanEnding(WeeklyPlan plan, LocalDate date) {
        members.findByWorkspaceIdOrderByJoinedAtAsc(plan.getProject().getWorkspace().getId()).stream()
                .map(WorkspaceMember::getUser)
                .forEach(user -> notifications.createAndSendDetailed(
                        user, "Kế hoạch tuần sắp kết thúc", plan.getName(), NotificationType.WEEKLY_PLAN_ENDING,
                        plan.getId(), plan.getProject().getId(), null,
                        "plan-ending:" + plan.getId() + ":" + user.getId() + ":" + date));
    }
}
