package com.teamspace.teamspace.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;

import com.teamspace.teamspace.exception.ForbiddenException;
import com.teamspace.teamspace.notification.entity.Notification;
import com.teamspace.teamspace.notification.enums.NotificationType;
import com.teamspace.teamspace.notification.repository.NotificationRepository;
import com.teamspace.teamspace.notification.service.NotificationService;
import com.teamspace.teamspace.project.entity.Project;
import com.teamspace.teamspace.project.repository.ProjectRepository;
import com.teamspace.teamspace.realtime.service.ProjectRealtimeService;
import com.teamspace.teamspace.task.entity.Task;
import com.teamspace.teamspace.task.entity.TaskWatcher;
import com.teamspace.teamspace.task.repository.TaskRepository;
import com.teamspace.teamspace.task.repository.TaskReviewHistoryRepository;
import com.teamspace.teamspace.task.repository.TaskWatcherRepository;
import com.teamspace.teamspace.task.service.TaskWatcherService;
import com.teamspace.teamspace.taskactivity.repository.TaskActivityRepository;
import com.teamspace.teamspace.taskactivity.service.TaskActivityService;
import com.teamspace.teamspace.user.entity.User;
import com.teamspace.teamspace.user.repository.UserRepository;
import com.teamspace.teamspace.workcategory.dto.WorkCategoryRequest;
import com.teamspace.teamspace.workcategory.entity.WorkCategory;
import com.teamspace.teamspace.workcategory.repository.WorkCategoryRepository;
import com.teamspace.teamspace.workcategory.service.WorkCategoryService;
import com.teamspace.teamspace.workspace.entity.Workspace;
import com.teamspace.teamspace.workspace.entity.WorkspaceMember;
import com.teamspace.teamspace.workspace.enums.WorkspaceRole;
import com.teamspace.teamspace.workspace.repository.WorkspaceMemberRepository;

@ExtendWith(MockitoExtension.class)
class TaskCollaborationServiceTest {
    @Mock TaskActivityRepository activityRepository;
    @Mock TaskReviewHistoryRepository reviewRepository;
    @Mock TaskRepository taskRepository;
    @Mock TaskWatcherRepository watcherRepository;
    @Mock WorkCategoryRepository categoryRepository;
    @Mock ProjectRepository projectRepository;
    @Mock UserRepository userRepository;
    @Mock WorkspaceMemberRepository memberRepository;
    @Mock ProjectRealtimeService realtime;
    @Mock NotificationRepository notificationRepository;
    @Mock SimpMessagingTemplate messagingTemplate;
    @Mock Authentication authentication;

    private User user;
    private Workspace workspace;
    private Project project;
    private Task task;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("member@example.com").fullName("Member").password("x").build();
        workspace = Workspace.builder().id(10L).createdBy(user).build();
        project = Project.builder().id(20L).workspace(workspace).createdBy(user).build();
        task = Task.builder().id(30L).project(project).createdBy(user).title("Task").build();
    }

    @Test
    void rejectsActivityTimelineForNonMember() {
        TaskActivityService service = new TaskActivityService(
                activityRepository, reviewRepository, taskRepository, userRepository, memberRepository);
        when(taskRepository.findById(30L)).thenReturn(Optional.of(task));
        when(authentication.getName()).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(memberRepository.existsByWorkspaceIdAndUserId(10L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> service.timeline(30L, 0, 20, authentication))
                .isInstanceOf(ForbiddenException.class);
        verify(activityRepository, never()).findByTaskId(any(), any());
    }

    @Test
    void followsAndUnfollowsTaskIdempotently() {
        TaskWatcherService service = new TaskWatcherService(
                watcherRepository, taskRepository, userRepository, memberRepository);
        TaskWatcher watcher = TaskWatcher.builder().id(40L).task(task).user(user).build();
        when(taskRepository.findById(30L)).thenReturn(Optional.of(task));
        when(authentication.getName()).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(memberRepository.existsByWorkspaceIdAndUserId(10L, 1L)).thenReturn(true);
        when(watcherRepository.findByTaskIdAndUserId(30L, 1L))
                .thenReturn(Optional.empty(), Optional.of(watcher));
        when(watcherRepository.save(any(TaskWatcher.class))).thenReturn(watcher);

        assertThat(service.follow(30L, authentication).getId()).isEqualTo(40L);
        service.unfollow(30L, authentication);

        verify(watcherRepository).save(any(TaskWatcher.class));
        verify(watcherRepository).delete(watcher);
    }

    @Test
    void rejectsDuplicateCategoryNameIgnoringCase() {
        WorkCategoryService service = new WorkCategoryService(categoryRepository, projectRepository, taskRepository,
                userRepository, memberRepository, realtime);
        WorkCategoryRequest request = new WorkCategoryRequest();
        request.setName("  THIẾT KẾ  ");
        request.setColor("#123456");
        request.setIcon("Palette");
        when(projectRepository.findById(20L)).thenReturn(Optional.of(project));
        when(authentication.getName()).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(memberRepository.findByWorkspaceIdAndUserId(10L, 1L)).thenReturn(Optional.of(
                WorkspaceMember.builder().workspace(workspace).user(user).role(WorkspaceRole.OWNER).build()));
        when(categoryRepository.findByProjectIdAndNormalizedName(20L, "thiết kế"))
                .thenReturn(Optional.of(WorkCategory.builder().id(50L).project(project).build()));

        assertThatThrownBy(() -> service.create(20L, request, authentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("đã tồn tại");
    }

    @Test
    void workCategoryServiceDoesNotExposeDeleteOperation() {
        assertThat(List.of(WorkCategoryService.class.getDeclaredMethods()).stream()
                .map(Method::getName))
                .doesNotContain("delete");
    }

    @Test
    void reusesNotificationWithSameDedupKey() {
        NotificationService service = new NotificationService(notificationRepository, userRepository, messagingTemplate);
        Notification existing = Notification.builder().id(60L).receiver(user).title("Existing").content("Content")
                .type(NotificationType.TASK_DUE_SOON).dedupKey("deadline:30:1").build();
        when(notificationRepository.findByDedupKey("deadline:30:1")).thenReturn(Optional.of(existing));

        var response = service.createAndSendDetailed(user, "Due", "Task", NotificationType.TASK_DUE_SOON,
                30L, 20L, 30L, "deadline:30:1");

        assertThat(response.getId()).isEqualTo(60L);
        verify(notificationRepository, never()).save(any());
        verify(messagingTemplate, never()).convertAndSend(any(String.class), any(Object.class));
    }
}
