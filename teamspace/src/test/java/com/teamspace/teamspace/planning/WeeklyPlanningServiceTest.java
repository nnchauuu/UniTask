package com.teamspace.teamspace.planning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import com.teamspace.teamspace.exception.ForbiddenException;
import com.teamspace.teamspace.planning.dto.CompleteWeeklyPlanRequest;
import com.teamspace.teamspace.planning.dto.MovePlanningTasksRequest;
import com.teamspace.teamspace.planning.dto.StartWeeklyPlanRequest;
import com.teamspace.teamspace.planning.dto.ReorderPlanTasksRequest;
import com.teamspace.teamspace.planning.entity.WeeklyPlan;
import com.teamspace.teamspace.planning.enums.IncompleteTaskAction;
import com.teamspace.teamspace.planning.enums.PlanningState;
import com.teamspace.teamspace.planning.enums.WeeklyPlanStatus;
import com.teamspace.teamspace.planning.repository.WeeklyPlanRepository;
import com.teamspace.teamspace.planning.repository.WeeklyPlanTaskSnapshotRepository;
import com.teamspace.teamspace.planning.service.WeeklyPlanningService;
import com.teamspace.teamspace.project.entity.Project;
import com.teamspace.teamspace.project.repository.ProjectRepository;
import com.teamspace.teamspace.task.entity.BoardColumn;
import com.teamspace.teamspace.task.entity.Task;
import com.teamspace.teamspace.task.enums.StatusGroup;
import com.teamspace.teamspace.task.enums.TaskPriority;
import com.teamspace.teamspace.task.enums.TaskReviewStatus;
import com.teamspace.teamspace.task.enums.TaskStatus;
import com.teamspace.teamspace.task.repository.TaskRepository;
import com.teamspace.teamspace.task.service.BoardService;
import com.teamspace.teamspace.task.service.TaskService;
import com.teamspace.teamspace.user.entity.User;
import com.teamspace.teamspace.user.repository.UserRepository;
import com.teamspace.teamspace.workspace.entity.Workspace;
import com.teamspace.teamspace.workspace.entity.WorkspaceMember;
import com.teamspace.teamspace.workspace.enums.WorkspaceRole;
import com.teamspace.teamspace.workspace.repository.WorkspaceMemberRepository;
import com.teamspace.teamspace.notification.service.NotificationService;
import com.teamspace.teamspace.realtime.service.ProjectRealtimeService;
import com.teamspace.teamspace.realtime.service.TaskChangeCoordinator;

@ExtendWith(MockitoExtension.class)
class WeeklyPlanningServiceTest {
    @Mock WeeklyPlanRepository planRepository;
    @Mock WeeklyPlanTaskSnapshotRepository snapshotRepository;
    @Mock TaskRepository taskRepository;
    @Mock ProjectRepository projectRepository;
    @Mock UserRepository userRepository;
    @Mock WorkspaceMemberRepository memberRepository;
    @Mock BoardService boardService;
    @Mock TaskService taskService;
    @Mock NotificationService notificationService;
    @Mock ProjectRealtimeService realtimeService;
    @Mock TaskChangeCoordinator taskChanges;
    @Mock Authentication authentication;

    private WeeklyPlanningService service;
    private User owner;
    private User memberUser;
    private Workspace workspace;
    private Project project;
    private BoardColumn todo;

    @BeforeEach
    void setUp() {
        service = new WeeklyPlanningService(planRepository, snapshotRepository, taskRepository, projectRepository, userRepository,
                memberRepository, boardService, taskService, notificationService, realtimeService, taskChanges);
        owner = User.builder().id(1L).email("owner@example.com").fullName("Owner").password("x").build();
        memberUser = User.builder().id(2L).email("member@example.com").fullName("Member").password("x").build();
        workspace = Workspace.builder().id(10L).name("Workspace").build();
        project = Project.builder().id(20L).name("Project").workspace(workspace).createdBy(owner).build();
        todo = BoardColumn.builder().id(30L).project(project).key("TODO").statusGroup(StatusGroup.TODO).build();
    }

    @Test
    void allowsOnlyOneActivePlanPerProject() {
        WeeklyPlan draft = plan(40L, WeeklyPlanStatus.DRAFT);
        WeeklyPlan active = plan(41L, WeeklyPlanStatus.ACTIVE);
        Task task = task(50L, null);
        authenticate(owner, WorkspaceRole.OWNER);
        when(planRepository.findByIdForUpdate(40L)).thenReturn(Optional.of(draft));
        when(taskRepository.findByWeeklyPlanIdOrderByBacklogPositionAscCreatedAtAsc(40L)).thenReturn(List.of(task));
        when(projectRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(project));
        when(planRepository.findActiveByProjectIdForUpdate(20L)).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> service.startPlan(40L, new StartWeeklyPlanRequest(), authentication))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("da co mot ke hoach");
    }

    @Test
    void memberCannotStartPlan() {
        WeeklyPlan draft = plan(40L, WeeklyPlanStatus.DRAFT);
        authenticate(memberUser, WorkspaceRole.MEMBER);
        when(planRepository.findByIdForUpdate(40L)).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> service.startPlan(40L, new StartWeeklyPlanRequest(), authentication))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void memberCannotCompletePlan() {
        WeeklyPlan active = plan(40L, WeeklyPlanStatus.ACTIVE);
        authenticate(memberUser, WorkspaceRole.MEMBER);
        when(planRepository.findByIdForUpdate(40L)).thenReturn(Optional.of(active));
        CompleteWeeklyPlanRequest request = new CompleteWeeklyPlanRequest();
        request.setAction(IncompleteTaskAction.MOVE_TO_UNPLANNED);

        assertThatThrownBy(() -> service.completePlan(40L, request, authentication))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void taskCannotBelongToTwoUnfinishedPlans() {
        WeeklyPlan target = plan(40L, WeeklyPlanStatus.DRAFT);
        WeeklyPlan other = plan(41L, WeeklyPlanStatus.DRAFT);
        Task task = task(50L, other);
        authenticate(owner, WorkspaceRole.OWNER);
        when(planRepository.findByIdForUpdate(40L)).thenReturn(Optional.of(target));
        when(taskRepository.findById(50L)).thenReturn(Optional.of(task));
        when(taskRepository.findByParentTaskIdOrderByCreatedAtAsc(50L)).thenReturn(List.of());
        MovePlanningTasksRequest request = new MovePlanningTasksRequest();
        request.setTaskIds(List.of(50L));

        assertThatThrownBy(() -> service.addTasks(40L, request, authentication))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("ke hoach chua hoan thanh khac");
    }

    @Test
    void deletingDraftReturnsTasksToUnplanned() {
        WeeklyPlan draft = plan(40L, WeeklyPlanStatus.DRAFT);
        Task task = task(50L, draft);
        authenticate(owner, WorkspaceRole.OWNER);
        when(planRepository.findByIdForUpdate(40L)).thenReturn(Optional.of(draft));
        when(taskRepository.findByWeeklyPlanIdOrderByBacklogPositionAscCreatedAtAsc(40L)).thenReturn(List.of(task));
        when(taskRepository.findByProjectIdAndPlanningStateOrderByBacklogPositionAscCreatedAtAsc(20L, PlanningState.UNPLANNED)).thenReturn(List.of());
        when(boardService.getDefaultColumn(project, StatusGroup.TODO)).thenReturn(todo);

        service.deletePlan(40L, authentication);

        assertThat(task.getPlanningState()).isEqualTo(PlanningState.UNPLANNED);
        assertThat(task.getWeeklyPlan()).isNull();
        assertThat(task.getBoardColumn()).isEqualTo(todo);
        verify(planRepository).delete(draft);
    }

    @Test
    void completingPlanCanReturnIncompleteTasksToUnplanned() {
        WeeklyPlan active = plan(40L, WeeklyPlanStatus.ACTIVE);
        Task task = task(50L, active);
        authenticate(owner, WorkspaceRole.OWNER);
        when(planRepository.findByIdForUpdate(40L)).thenReturn(Optional.of(active));
        when(taskRepository.findByWeeklyPlanIdOrderByBacklogPositionAscCreatedAtAsc(40L)).thenReturn(List.of(task));
        when(taskRepository.findByProjectIdAndPlanningStateOrderByBacklogPositionAscCreatedAtAsc(20L, PlanningState.UNPLANNED)).thenReturn(List.of());
        when(boardService.getDefaultColumn(project, StatusGroup.TODO)).thenReturn(todo);
        CompleteWeeklyPlanRequest request = new CompleteWeeklyPlanRequest();
        request.setAction(IncompleteTaskAction.MOVE_TO_UNPLANNED);

        service.completePlan(40L, request, authentication);

        assertThat(active.getStatus()).isEqualTo(WeeklyPlanStatus.COMPLETED);
        assertThat(task.getPlanningState()).isEqualTo(PlanningState.UNPLANNED);
        assertThat(task.getWeeklyPlan()).isNull();
        verify(snapshotRepository).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void childCannotMoveToDifferentPlanThanParent() {
        WeeklyPlan target = plan(40L, WeeklyPlanStatus.DRAFT);
        Task parent = task(51L, null);
        parent.setPlanningState(PlanningState.UNPLANNED);
        Task child = task(50L, null);
        child.setParentTask(parent);
        authenticate(owner, WorkspaceRole.OWNER);
        when(planRepository.findByIdForUpdate(40L)).thenReturn(Optional.of(target));
        when(taskRepository.findById(50L)).thenReturn(Optional.of(child));
        when(taskRepository.findByParentTaskIdOrderByCreatedAtAsc(50L)).thenReturn(List.of());
        MovePlanningTasksRequest request = new MovePlanningTasksRequest();
        request.setTaskIds(List.of(50L));

        assertThatThrownBy(() -> service.addTasks(40L, request, authentication))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("task cha");
    }

    @Test
    void overloadedPlanRequiresExplicitConfirmation() {
        WeeklyPlan draft = plan(40L, WeeklyPlanStatus.DRAFT);
        draft.setCapacity(BigDecimal.valueOf(4));
        Task task = task(50L, draft);
        task.setEstimatedEffort(BigDecimal.valueOf(8));
        authenticate(owner, WorkspaceRole.OWNER);
        when(planRepository.findByIdForUpdate(40L)).thenReturn(Optional.of(draft));
        when(taskRepository.findByWeeklyPlanIdOrderByBacklogPositionAscCreatedAtAsc(40L)).thenReturn(List.of(task));

        assertThatThrownBy(() -> service.startPlan(40L, new StartWeeklyPlanRequest(), authentication))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("vuot suc chua");
    }

    @Test
    void managerCanReorderAllTasksInDraft() {
        WeeklyPlan draft = plan(40L, WeeklyPlanStatus.DRAFT);
        Task first = task(50L, draft); Task second = task(51L, draft);
        authenticate(owner, WorkspaceRole.OWNER);
        when(planRepository.findByIdForUpdate(40L)).thenReturn(Optional.of(draft));
        when(taskRepository.findByWeeklyPlanIdOrderByBacklogPositionAscCreatedAtAsc(40L)).thenReturn(List.of(first, second));
        ReorderPlanTasksRequest request = new ReorderPlanTasksRequest(); request.setTaskIds(List.of(51L, 50L));

        service.reorderPlanTasks(40L, request, authentication);

        assertThat(second.getBacklogPosition()).isZero();
        assertThat(first.getBacklogPosition()).isEqualTo(1L);
    }

    @Test
    void addingTaskToActivePlanKeepsItsSelectedBoardColumn() {
        WeeklyPlan active = plan(40L, WeeklyPlanStatus.ACTIVE);
        BoardColumn firstColumn = BoardColumn.builder().id(31L).project(project).key("DOING")
                .statusGroup(StatusGroup.IN_PROGRESS).build();
        Task task = task(50L, null);
        task.setBoardColumn(firstColumn);
        task.setStatus(TaskStatus.IN_PROGRESS);
        authenticate(owner, WorkspaceRole.OWNER);
        when(planRepository.findByIdForUpdate(40L)).thenReturn(Optional.of(active));
        when(taskRepository.findById(50L)).thenReturn(Optional.of(task));
        when(taskRepository.findByParentTaskIdOrderByCreatedAtAsc(50L)).thenReturn(List.of());
        when(taskRepository.findByWeeklyPlanIdOrderByBacklogPositionAscCreatedAtAsc(40L)).thenReturn(List.of(task));
        MovePlanningTasksRequest request = new MovePlanningTasksRequest();
        request.setTaskIds(List.of(50L));

        service.addTasks(40L, request, authentication);

        assertThat(task.getPlanningState()).isEqualTo(PlanningState.ACTIVE);
        assertThat(task.getWeeklyPlan()).isSameAs(active);
        assertThat(task.getBoardColumn()).isSameAs(firstColumn);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    private void authenticate(User user, WorkspaceRole role) {
        when(authentication.getName()).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(memberRepository.findByWorkspaceIdAndUserId(10L, user.getId())).thenReturn(Optional.of(
                WorkspaceMember.builder().id(100L + user.getId()).workspace(workspace).user(user).role(role).build()));
    }

    private WeeklyPlan plan(Long id, WeeklyPlanStatus status) {
        return WeeklyPlan.builder().id(id).project(project).name("Week").startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(6)).status(status).createdBy(owner)
                .estimateUnit(com.teamspace.teamspace.planning.enums.EstimateUnit.HOURS).version(0L).build();
    }

    private Task task(Long id, WeeklyPlan plan) {
        return Task.builder().id(id).project(project).title("Task").createdBy(owner).assignedTo(memberUser)
                .status(TaskStatus.TODO).priority(TaskPriority.MEDIUM).type("DESIGN")
                .planningState(plan == null ? PlanningState.UNPLANNED : plan.getStatus() == WeeklyPlanStatus.ACTIVE ? PlanningState.ACTIVE : PlanningState.PLANNED)
                .weeklyPlan(plan).reviewStatus(TaskReviewStatus.NONE).boardColumn(todo).build();
    }
}
