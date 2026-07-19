package com.teamspace.teamspace.realtime.service;

import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.teamspace.teamspace.notification.enums.NotificationType;
import com.teamspace.teamspace.notification.service.NotificationService;
import com.teamspace.teamspace.task.entity.Task;
import com.teamspace.teamspace.task.repository.TaskWatcherRepository;
import com.teamspace.teamspace.taskactivity.service.TaskActivityService;
import com.teamspace.teamspace.user.entity.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TaskChangeCoordinator {
    private final TaskActivityService activity;
    private final ProjectRealtimeService realtime;
    private final TaskWatcherRepository watchers;
    private final NotificationService notifications;

    public void changed(Task task, User actor, String event, String field, String oldValue, String newValue,
            String description, boolean notifyWatchers) {
        activity.log(task, actor, event, field, oldValue, newValue, description);
        realtime.publish(task.getProject().getId(), task.getId(), event, task.getVersion(),
                Map.of("field", field == null ? "" : field));
        if (!notifyWatchers) return;

        watchers.findByTaskIdOrderByCreatedAtAsc(task.getId()).stream()
                .map(watcher -> watcher.getUser())
                .filter(user -> actor == null || !Objects.equals(user.getId(), actor.getId()))
                .forEach(user -> notifications.createAndSendDetailed(
                        user, "Công việc đã thay đổi", description, NotificationType.TASK_COMMENT,
                        task.getId(), task.getProject().getId(), task.getId(),
                        "watch-update:" + task.getId() + ":" + task.getVersion() + ":" + event + ":"
                                + String.valueOf(field) + ":" + user.getId()));
    }

    public void event(Task task, String event) {
        realtime.publish(task.getProject().getId(), task.getId(), event, task.getVersion(), Map.of());
    }
}
