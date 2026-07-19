package com.teamspace.teamspace.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.inOrder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import com.teamspace.teamspace.activity.service.ActivityLogService;
import com.teamspace.teamspace.notification.service.NotificationService;
import com.teamspace.teamspace.project.entity.Project;
import com.teamspace.teamspace.project.repository.ProjectRepository;
import com.teamspace.teamspace.task.dto.CreateChecklistItemRequest;
import com.teamspace.teamspace.task.dto.CreateTaskCommentRequest;
import com.teamspace.teamspace.task.dto.CreateTaskRequest;
import com.teamspace.teamspace.task.dto.ReorderChecklistRequest;
import com.teamspace.teamspace.task.dto.UpdateTaskRequest;
import com.teamspace.teamspace.task.entity.BoardColumn;
import com.teamspace.teamspace.task.entity.Task;
import com.teamspace.teamspace.task.entity.TaskChecklistItem;
import com.teamspace.teamspace.task.enums.StatusGroup;
import com.teamspace.teamspace.task.enums.SubtaskDeleteAction;
import com.teamspace.teamspace.task.enums.TaskPriority;
import com.teamspace.teamspace.task.enums.TaskReviewStatus;
import com.teamspace.teamspace.task.enums.TaskStatus;
import com.teamspace.teamspace.task.repository.TaskChecklistItemRepository;
import com.teamspace.teamspace.task.repository.TaskCommentRepository;
import com.teamspace.teamspace.task.repository.TaskRepository;
import com.teamspace.teamspace.task.service.BoardService;
import com.teamspace.teamspace.task.service.TaskService;
import com.teamspace.teamspace.task.service.TaskDeletionService;
import com.teamspace.teamspace.task.service.TaskWatcherService;
import com.teamspace.teamspace.task.repository.TaskWatcherRepository;
import com.teamspace.teamspace.user.entity.User;
import com.teamspace.teamspace.user.repository.UserRepository;
import com.teamspace.teamspace.workspace.entity.Workspace;
import com.teamspace.teamspace.workspace.entity.WorkspaceMember;
import com.teamspace.teamspace.workspace.enums.WorkspaceRole;
import com.teamspace.teamspace.workspace.repository.WorkspaceMemberRepository;
import com.teamspace.teamspace.planning.entity.WeeklyPlan;
import com.teamspace.teamspace.planning.enums.PlanningState;
import com.teamspace.teamspace.workcategory.entity.WorkCategory;
import com.teamspace.teamspace.workcategory.service.WorkCategoryService;
import com.teamspace.teamspace.realtime.service.TaskChangeCoordinator;

@ExtendWith(MockitoExtension.class)
class TaskHierarchyServiceTest {

    @Mock TaskRepository taskRepository;
    @Mock TaskCommentRepository taskCommentRepository;
    @Mock TaskChecklistItemRepository checklistRepository;
    @Mock ProjectRepository projectRepository;
    @Mock WorkspaceMemberRepository memberRepository;
    @Mock UserRepository userRepository;
    @Mock NotificationService notificationService;
    @Mock ActivityLogService activityLogService;
    @Mock BoardService boardService;
    @Mock WorkCategoryService workCategoryService;
    @Mock TaskChangeCoordinator taskChanges;
    @Mock TaskWatcherService watcherService;
    @Mock TaskWatcherRepository taskWatcherRepository;
    @Mock TaskDeletionService taskDeletionService;
    @Mock Authentication authentication;

    private TaskService service;
    private User user;
    private Project project;

    @BeforeEach
    void setUp() {
        service = new TaskService(taskRepository, taskCommentRepository, checklistRepository, projectRepository,
                memberRepository, userRepository, notificationService, activityLogService, boardService, workCategoryService,
                taskChanges, watcherService, taskWatcherRepository, taskDeletionService);
        user = User.builder().id(1L).email("student@example.com").fullName("Student").password("x").build();
        Workspace workspace = Workspace.builder().id(10L).build();
        project = Project.builder().id(20L).workspace(workspace).createdBy(user).build();
        when(authentication.getName()).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(memberRepository.findByWorkspaceIdAndUserId(10L, 1L)).thenReturn(Optional.of(
                WorkspaceMember.builder().user(user).workspace(workspace).role(WorkspaceRole.MEMBER).build()
        ));
    }

    @Test
    void rejectsCreatingAThirdTaskLevel() {
        Task root = Task.builder().id(30L).project(project).createdBy(user).build();
        Task child = Task.builder().id(31L).project(project).parentTask(root).createdBy(user).build();
        when(projectRepository.findById(20L)).thenReturn(Optional.of(project));
        when(taskRepository.findById(31L)).thenReturn(Optional.of(child));

        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Third level");
        request.setParentTaskId(31L);

        assertThatThrownBy(() -> service.createTask(20L, request, authentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("khong the co them task con");
    }

    @Test
    void subtaskInheritsParentPlanningContextAndPersistsNewDetailFields() {
        WeeklyPlan plan = WeeklyPlan.builder().id(25L).project(project).name("Week 1").build();
        Task parent = Task.builder().id(30L).project(project).title("Parent").createdBy(user)
                .planningState(PlanningState.PLANNED).weeklyPlan(plan).build();
        BoardColumn todo = BoardColumn.builder().id(40L).project(project).key("todo").label("Can lam")
                .statusGroup(StatusGroup.TODO).build();
        WorkCategory category = WorkCategory.builder().id(50L).project(project).name("Backend")
                .color("#123456").icon("Code2").active(true).createdBy(user).build();
        LocalDate startDate = LocalDate.of(2026, 7, 16);
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Child");
        request.setParentTaskId(parent.getId());
        request.setWorkCategoryId(category.getId());
        request.setType(category.getName());
        request.setStartDate(startDate);
        request.setLabels("  backend, api  ");

        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(taskRepository.findById(parent.getId())).thenReturn(Optional.of(parent));
        when(workCategoryService.resolve(project, category.getId(), category.getName())).thenReturn(category);
        when(boardService.resolveColumn(project, null, TaskStatus.TODO)).thenReturn(todo);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task saved = invocation.getArgument(0);
            saved.setId(31L);
            return saved;
        });

        var response = service.createTask(project.getId(), request, authentication);

        assertThat(response.getPlanningState()).isEqualTo(PlanningState.PLANNED);
        assertThat(response.getWeeklyPlanId()).isEqualTo(plan.getId());
        assertThat(response.getStartDate()).isEqualTo(startDate);
        assertThat(response.getLabels()).isEqualTo("backend, api");
    }

    @Test
    void updateStatusResolvesACompatibleColumnInsteadOfKeepingStaleColumnId() {
        BoardColumn inProgress = BoardColumn.builder().id(40L).project(project).key("doing").label("Dang lam")
                .statusGroup(StatusGroup.IN_PROGRESS).build();
        BoardColumn done = BoardColumn.builder().id(41L).project(project).key("done").label("Hoan thanh")
                .statusGroup(StatusGroup.DONE).build();
        WorkCategory category = WorkCategory.builder().id(50L).project(project).name("Backend")
                .color("#123456").icon("Code2").active(true).createdBy(user).build();
        Task task = Task.builder().id(60L).project(project).title("Task").description("Old")
                .assignedTo(user).createdBy(user).planningState(PlanningState.ACTIVE)
                .boardColumn(inProgress).status(TaskStatus.IN_PROGRESS).priority(TaskPriority.MEDIUM)
                .type(category.getName()).workCategory(category).reviewRequired(true)
                .reviewStatus(TaskReviewStatus.NONE).build();
        LocalDate startDate = LocalDate.of(2026, 7, 17);
        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setTitle("Task updated");
        request.setDescription("New");
        request.setAssignedToUserId(user.getId());
        request.setBoardColumnId(inProgress.getId());
        request.setStatus(TaskStatus.DONE);
        request.setPriority(TaskPriority.HIGH);
        request.setType(category.getName());
        request.setWorkCategoryId(category.getId());
        request.setReviewRequired(true);
        request.setStartDate(startDate);
        request.setLabels("backend, urgent");

        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(memberRepository.existsByWorkspaceIdAndUserId(10L, user.getId())).thenReturn(true);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(workCategoryService.resolve(project, category.getId(), category.getName())).thenReturn(category);
        when(boardService.resolveColumn(project, inProgress.getId(), TaskStatus.DONE)).thenReturn(inProgress);
        when(boardService.resolveColumn(project, null, TaskStatus.DONE)).thenReturn(done);
        when(boardService.nextTaskPosition(done)).thenReturn(7L);

        var response = service.updateTask(task.getId(), request, authentication);

        assertThat(response.getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(response.getStatusGroup()).isEqualTo(StatusGroup.DONE);
        assertThat(response.getBoardColumnId()).isEqualTo(done.getId());
        assertThat(response.getBoardPosition()).isEqualTo(7L);
        assertThat(response.getStartDate()).isEqualTo(startDate);
        assertThat(response.getLabels()).isEqualTo("backend, urgent");
        verify(boardService).validateBoardTransition(task, done);
        verify(boardService).nextTaskPosition(done);
    }

    @Test
    void cleansChildDependenciesBeforeDeletingParentTree() {
        Task parent = Task.builder().id(70L).project(project).createdBy(user).build();
        Task child = Task.builder().id(71L).project(project).parentTask(parent).createdBy(user).build();
        when(taskRepository.findById(parent.getId())).thenReturn(Optional.of(parent));
        when(taskRepository.findByParentTaskIdOrderByCreatedAtAsc(parent.getId())).thenReturn(List.of(child));

        service.deleteTask(parent.getId(), SubtaskDeleteAction.DELETE, authentication);

        var ordered = inOrder(taskDeletionService, taskRepository);
        ordered.verify(taskDeletionService).cleanup(child);
        ordered.verify(taskRepository).deleteAll(List.of(child));
        ordered.verify(taskRepository).flush();
        ordered.verify(taskDeletionService).cleanup(parent);
        ordered.verify(taskRepository).delete(parent);
    }

    @Test
    void detachesChildrenBeforeDeletingParent() {
        Task parent = Task.builder().id(40L).project(project).createdBy(user).build();
        Task child = Task.builder().id(41L).project(project).parentTask(parent).createdBy(user).build();
        when(taskRepository.findById(40L)).thenReturn(Optional.of(parent));
        when(taskRepository.findByParentTaskIdOrderByCreatedAtAsc(40L)).thenReturn(List.of(child));

        service.deleteTask(40L, SubtaskDeleteAction.DETACH, authentication);

        assertThat(child.getParentTask()).isNull();
        verify(taskRepository).saveAll(List.of(child));
        verify(taskRepository).delete(parent);
    }

    @Test
    void createsPersistentChecklistItemAtNextPosition() {
        Task task = Task.builder().id(50L).project(project).createdBy(user).build();
        when(taskRepository.findById(50L)).thenReturn(Optional.of(task));
        when(checklistRepository.countByTaskId(50L)).thenReturn(2L);
        when(checklistRepository.save(any(TaskChecklistItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CreateChecklistItemRequest request = new CreateChecklistItemRequest();
        request.setContent("  Kiểm tra báo cáo  ");

        var response = service.createChecklistItem(50L, request, authentication);

        assertThat(response.getContent()).isEqualTo("Kiểm tra báo cáo");
        assertThat(response.getPosition()).isEqualTo(2);
    }

    @Test
    void reordersEveryChecklistItemIntoContinuousPositions() {
        Task task = Task.builder().id(60L).project(project).createdBy(user).build();
        TaskChecklistItem first = TaskChecklistItem.builder().id(61L).task(task).content("First").position(8).build();
        TaskChecklistItem second = TaskChecklistItem.builder().id(62L).task(task).content("Second").position(3).build();
        when(taskRepository.findById(60L)).thenReturn(Optional.of(task));
        when(checklistRepository.findByTaskIdOrderByPositionAscIdAsc(60L)).thenReturn(List.of(second, first));
        ReorderChecklistRequest request = new ReorderChecklistRequest();
        request.setItemIds(List.of(61L, 62L));

        service.reorderChecklist(60L, request, authentication);

        assertThat(first.getPosition()).isZero();
        assertThat(second.getPosition()).isEqualTo(1);
    }

    @Test
    void rejectsChecklistOrderWithDuplicatesOrMissingItems() {
        Task task = Task.builder().id(70L).project(project).createdBy(user).build();
        TaskChecklistItem first = TaskChecklistItem.builder().id(71L).task(task).content("First").position(0).build();
        TaskChecklistItem second = TaskChecklistItem.builder().id(72L).task(task).content("Second").position(1).build();
        when(taskRepository.findById(70L)).thenReturn(Optional.of(task));
        when(checklistRepository.findByTaskIdOrderByPositionAscIdAsc(70L)).thenReturn(List.of(first, second));
        ReorderChecklistRequest request = new ReorderChecklistRequest();
        request.setItemIds(List.of(71L, 71L));

        assertThatThrownBy(() -> service.reorderChecklist(70L, request, authentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("khong hop le");
    }

    @Test
    void boardTaskEndpointOnlyReturnsActivePlanningTasks() {
        when(projectRepository.findById(20L)).thenReturn(Optional.of(project));
        when(taskRepository.findByProjectIdAndPlanningStateOrderByBacklogPositionAscCreatedAtAsc(20L, PlanningState.ACTIVE))
                .thenReturn(List.of());

        assertThat(service.getProjectTasks(20L, authentication)).isEmpty();

        verify(taskRepository).findByProjectIdAndPlanningStateOrderByBacklogPositionAscCreatedAtAsc(20L, PlanningState.ACTIVE);
    }

    @Test
    void rejectsMentioningUserOutsideProject() {
        User outsider = User.builder().id(99L).email("outside@example.com").fullName("Outside").password("x").build();
        Task task = Task.builder().id(80L).project(project).createdBy(user).title("Task").build();
        CreateTaskCommentRequest request = new CreateTaskCommentRequest();
        request.setContent("Please review");
        request.setMentionedUserIds(List.of(99L, 99L));
        when(taskRepository.findById(80L)).thenReturn(Optional.of(task));
        when(userRepository.findAllById(any())).thenReturn(List.of(outsider));
        when(memberRepository.existsByWorkspaceIdAndUserId(10L, 99L)).thenReturn(false);

        assertThatThrownBy(() -> service.createComment(80L, request, authentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mention");
        verify(taskCommentRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void sendsOnlyOneNotificationForDuplicateMentionIds() {
        User mentioned = User.builder().id(2L).email("member2@example.com").fullName("Member 2").password("x").build();
        Task task = Task.builder().id(90L).project(project).createdBy(user).title("Task").build();
        CreateTaskCommentRequest request = new CreateTaskCommentRequest();
        request.setContent("Please review");
        request.setMentionedUserIds(List.of(2L, 2L));
        when(taskRepository.findById(90L)).thenReturn(Optional.of(task));
        when(userRepository.findAllById(any())).thenReturn(List.of(mentioned));
        when(memberRepository.existsByWorkspaceIdAndUserId(10L, 2L)).thenReturn(true);
        when(taskCommentRepository.save(any())).thenAnswer(invocation -> {
            var comment = invocation.getArgument(0, com.teamspace.teamspace.task.entity.TaskComment.class);
            comment.setId(91L);
            return comment;
        });
        when(taskWatcherRepository.findByTaskIdOrderByCreatedAtAsc(90L)).thenReturn(List.of());

        service.createComment(90L, request, authentication);

        verify(notificationService, org.mockito.Mockito.times(1)).createAndSendDetailed(
                org.mockito.ArgumentMatchers.eq(mentioned), any(), any(),
                org.mockito.ArgumentMatchers.eq(com.teamspace.teamspace.notification.enums.NotificationType.TASK_MENTION),
                org.mockito.ArgumentMatchers.eq(90L), org.mockito.ArgumentMatchers.eq(20L),
                org.mockito.ArgumentMatchers.eq(90L), org.mockito.ArgumentMatchers.eq("mention:91:2"));
    }
}
