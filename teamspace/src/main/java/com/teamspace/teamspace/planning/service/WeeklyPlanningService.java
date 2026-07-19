package com.teamspace.teamspace.planning.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.LinkedHashMap;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamspace.teamspace.exception.ForbiddenException;
import com.teamspace.teamspace.exception.ResourceNotFoundException;
import com.teamspace.teamspace.exception.UnauthorizedException;
import com.teamspace.teamspace.planning.dto.CompleteWeeklyPlanRequest;
import com.teamspace.teamspace.planning.dto.CreateWeeklyPlanRequest;
import com.teamspace.teamspace.planning.dto.MovePlanningTasksRequest;
import com.teamspace.teamspace.planning.dto.ReorderUnplannedTasksRequest;
import com.teamspace.teamspace.planning.dto.ReorderPlanTasksRequest;
import com.teamspace.teamspace.planning.dto.MemberWorkloadResponse;
import com.teamspace.teamspace.planning.dto.PlanTaskSnapshotResponse;
import com.teamspace.teamspace.planning.dto.StartWeeklyPlanRequest;
import com.teamspace.teamspace.planning.dto.UpdateWeeklyPlanRequest;
import com.teamspace.teamspace.planning.dto.WeeklyPlanCompletionResponse;
import com.teamspace.teamspace.planning.dto.WeeklyPlanResponse;
import com.teamspace.teamspace.planning.entity.WeeklyPlan;
import com.teamspace.teamspace.planning.enums.IncompleteTaskAction;
import com.teamspace.teamspace.planning.enums.PlanningState;
import com.teamspace.teamspace.planning.enums.WeeklyPlanStatus;
import com.teamspace.teamspace.planning.repository.WeeklyPlanRepository;
import com.teamspace.teamspace.planning.repository.WeeklyPlanTaskSnapshotRepository;
import com.teamspace.teamspace.planning.entity.WeeklyPlanTaskSnapshot;
import com.teamspace.teamspace.project.entity.Project;
import com.teamspace.teamspace.project.repository.ProjectRepository;
import com.teamspace.teamspace.task.dto.CreateTaskRequest;
import com.teamspace.teamspace.task.dto.TaskResponse;
import com.teamspace.teamspace.task.entity.Task;
import com.teamspace.teamspace.task.enums.StatusGroup;
import com.teamspace.teamspace.task.enums.TaskStatus;
import com.teamspace.teamspace.notification.enums.NotificationType;
import com.teamspace.teamspace.notification.service.NotificationService;
import com.teamspace.teamspace.realtime.service.ProjectRealtimeService;
import com.teamspace.teamspace.realtime.service.TaskChangeCoordinator;
import com.teamspace.teamspace.task.repository.TaskRepository;
import com.teamspace.teamspace.task.service.BoardService;
import com.teamspace.teamspace.task.service.TaskService;
import com.teamspace.teamspace.user.entity.User;
import com.teamspace.teamspace.user.repository.UserRepository;
import com.teamspace.teamspace.workspace.entity.WorkspaceMember;
import com.teamspace.teamspace.workspace.enums.WorkspaceRole;
import com.teamspace.teamspace.workspace.repository.WorkspaceMemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WeeklyPlanningService {
    private final WeeklyPlanRepository planRepository;
    private final WeeklyPlanTaskSnapshotRepository snapshotRepository;
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final BoardService boardService;
    private final TaskService taskService;
    private final NotificationService notificationService;
    private final ProjectRealtimeService realtimeService;
    private final TaskChangeCoordinator taskChanges;

    @Transactional(readOnly = true)
    public List<WeeklyPlanResponse> listPlans(Long projectId, Authentication authentication) {
        Project project = project(projectId);
        WorkspaceMember member = member(project, currentUser(authentication));
        return planRepository.findByProjectIdOrderByStartDateDescCreatedAtDesc(projectId).stream()
                .map(plan -> response(plan, member.getRole())).toList();
    }

    @Transactional(readOnly = true)
    public WeeklyPlanResponse getPlan(Long planId, Authentication authentication) {
        WeeklyPlan plan = plan(planId);
        WorkspaceMember member = member(plan.getProject(), currentUser(authentication));
        return response(plan, member.getRole());
    }

    @Transactional
    public WeeklyPlanResponse createPlan(Long projectId, CreateWeeklyPlanRequest request, Authentication authentication) {
        Project project = project(projectId);
        User actor = currentUser(authentication);
        WorkspaceMember member = member(project, actor);
        requireManager(member);
        validateDates(request.getStartDate(), request.getEndDate());
        WeeklyPlan plan = WeeklyPlan.builder().project(project).name(request.getName().trim())
                .goal(normalize(request.getGoal())).description(normalize(request.getDescription()))
                .capacity(request.getCapacity()).estimateUnit(request.getEstimateUnit())
                .startDate(request.getStartDate()).endDate(request.getEndDate())
                .status(WeeklyPlanStatus.DRAFT).createdBy(actor).build();
        return response(planRepository.save(plan), member.getRole());
    }

    @Transactional
    public WeeklyPlanResponse updatePlan(Long planId, UpdateWeeklyPlanRequest request, Authentication authentication) {
        WeeklyPlan plan = planRepository.findByIdForUpdate(planId).orElseThrow(() -> notFound("Khong tim thay ke hoach"));
        WorkspaceMember member = member(plan.getProject(), currentUser(authentication));
        requireManager(member);
        requireDraft(plan);
        checkVersion(plan, request.getVersion());
        validateDates(request.getStartDate(), request.getEndDate());
        plan.setName(request.getName().trim());
        plan.setGoal(normalize(request.getGoal()));
        plan.setDescription(normalize(request.getDescription()));
        plan.setCapacity(request.getCapacity());
        plan.setEstimateUnit(request.getEstimateUnit());
        plan.setStartDate(request.getStartDate());
        plan.setEndDate(request.getEndDate());
        return response(plan, member.getRole());
    }

    @Transactional
    public void deletePlan(Long planId, Authentication authentication) {
        WeeklyPlan plan = planRepository.findByIdForUpdate(planId).orElseThrow(() -> notFound("Khong tim thay ke hoach"));
        requireManager(member(plan.getProject(), currentUser(authentication)));
        requireDraft(plan);
        List<Task> tasks = taskRepository.findByWeeklyPlanIdOrderByBacklogPositionAscCreatedAtAsc(planId);
        moveTasks(tasks, PlanningState.UNPLANNED, null);
        tasks.forEach(task -> taskChanges.changed(task, currentUser(authentication), "TASK_ARCHIVED", "planningState", "ACTIVE", "UNPLANNED", "Đưa công việc vào danh sách chưa lên kế hoạch", false));
        planRepository.delete(plan);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getUnplanned(Long projectId, Authentication authentication) {
        Project project = project(projectId);
        WorkspaceMember member = member(project, currentUser(authentication));
        return taskRepository.findByProjectIdAndPlanningStateOrderByBacklogPositionAscCreatedAtAsc(projectId, PlanningState.UNPLANNED)
                .stream().map(task -> TaskResponse.from(task, member.getRole())).toList();
    }

    @Transactional
    public TaskResponse createUnplanned(Long projectId, CreateTaskRequest request, Authentication authentication) {
        TaskResponse created = taskService.createTask(projectId, request, authentication);
        Task task = task(created.getId());
        WorkspaceMember member = member(task.getProject(), currentUser(authentication));
        task.setPlanningState(PlanningState.UNPLANNED);
        task.setWeeklyPlan(null);
        task.setBacklogPosition(nextUnplannedPosition(projectId));
        return TaskResponse.from(task, member.getRole());
    }

    @Transactional
    public List<TaskResponse> reorderUnplanned(Long projectId, ReorderUnplannedTasksRequest request, Authentication authentication) {
        Project project = project(projectId);
        WorkspaceMember member = member(project, currentUser(authentication));
        List<Task> tasks = taskRepository.findByProjectIdAndPlanningStateOrderByBacklogPositionAscCreatedAtAsc(projectId, PlanningState.UNPLANNED);
        Set<Long> currentIds = tasks.stream().map(Task::getId).collect(java.util.stream.Collectors.toSet());
        if (request.getTaskIds().size() != tasks.size() || new HashSet<>(request.getTaskIds()).size() != tasks.size()
                || !currentIds.equals(new HashSet<>(request.getTaskIds()))) {
            throw new IllegalArgumentException("Danh sach sap xep cong viec chua len ke hoach khong hop le");
        }
        var byId = tasks.stream().collect(java.util.stream.Collectors.toMap(Task::getId, item -> item));
        for (int index = 0; index < request.getTaskIds().size(); index++) byId.get(request.getTaskIds().get(index)).setBacklogPosition((long) index);
        return request.getTaskIds().stream().map(byId::get).map(task -> TaskResponse.from(task, member.getRole())).toList();
    }

    @Transactional
    public List<TaskResponse> moveToUnplanned(Long taskId, boolean includeSubtasks, Authentication authentication) {
        Task root = task(taskId);
        if (root.getPlanningState() != PlanningState.ACTIVE) throw new IllegalArgumentException("Chi task tren Board moi duoc dua ve danh sach chua len ke hoach");
        WorkspaceMember member = requireCanMove(root, currentUser(authentication));
        List<Task> tasks = familyForMove(root, includeSubtasks);
        validateParentDestination(tasks, PlanningState.UNPLANNED, null);
        moveTasks(tasks, PlanningState.UNPLANNED, null);
        return tasks.stream().map(task -> TaskResponse.from(task, member.getRole())).toList();
    }

    @Transactional
    public List<TaskResponse> moveToBoard(Long taskId, boolean includeSubtasks, Authentication authentication) {
        Task root = task(taskId);
        if (root.getPlanningState() != PlanningState.UNPLANNED) throw new IllegalArgumentException("Chi task chua len ke hoach moi duoc dua truc tiep vao Board");
        WorkspaceMember member = requireCanMove(root, currentUser(authentication));
        List<Task> tasks = familyForMove(root, includeSubtasks);
        WeeklyPlan active = planRepository.findByProjectIdAndStatus(root.getProject().getId(), WeeklyPlanStatus.ACTIVE).orElse(null);
        validateParentDestination(tasks, PlanningState.ACTIVE, active);
        moveTasks(tasks, PlanningState.ACTIVE, active);
        tasks.forEach(task -> taskChanges.changed(task, currentUser(authentication), "TASK_UPDATED", "planningState", "UNPLANNED", "ACTIVE", "Đưa công việc vào Board", false));
        return tasks.stream().map(task -> TaskResponse.from(task, member.getRole())).toList();
    }

    @Transactional
    public WeeklyPlanResponse addTasks(Long planId, MovePlanningTasksRequest request, Authentication authentication) {
        WeeklyPlan plan = planRepository.findByIdForUpdate(planId).orElseThrow(() -> notFound("Khong tim thay ke hoach"));
        WorkspaceMember member = member(plan.getProject(), currentUser(authentication));
        requireManager(member);
        requireSchedulable(plan);
        List<Task> roots = request.getTaskIds().stream().distinct().map(this::task).toList();
        List<Task> tasks = expandFamilies(roots, request.isIncludeSubtasks());
        tasks.forEach(task -> {
            if (!Objects.equals(task.getProject().getId(), plan.getProject().getId())) throw new IllegalArgumentException("Task va ke hoach phai cung du an");
            if (task.getWeeklyPlan() != null && task.getWeeklyPlan().getStatus() != WeeklyPlanStatus.COMPLETED
                    && !Objects.equals(task.getWeeklyPlan().getId(), planId)) {
                throw new IllegalArgumentException("Task da thuoc mot ke hoach chua hoan thanh khac");
            }
            if (task.getPlanningState() != PlanningState.UNPLANNED
                    && !(task.getPlanningState() == PlanningState.PLANNED && task.getWeeklyPlan() != null && Objects.equals(task.getWeeklyPlan().getId(), planId))) {
                throw new IllegalArgumentException("Chi task chua len ke hoach moi duoc them vao ke hoach");
            }
        });
        PlanningState destination = plan.getStatus() == WeeklyPlanStatus.ACTIVE ? PlanningState.ACTIVE : PlanningState.PLANNED;
        validateParentDestination(tasks, destination, plan);
        moveTasks(tasks, destination, plan);
        User actor = member.getUser();
        tasks.forEach(task -> taskChanges.changed(task, actor, "TASK_UPDATED", "weeklyPlan",
                null, plan.getName(), "Đưa công việc vào kế hoạch tuần", true));
        return response(plan, member.getRole());
    }

    @Transactional
    public WeeklyPlanResponse reorderPlanTasks(Long planId, ReorderPlanTasksRequest request, Authentication authentication) {
        WeeklyPlan plan = planRepository.findByIdForUpdate(planId).orElseThrow(() -> notFound("Khong tim thay ke hoach"));
        WorkspaceMember member = member(plan.getProject(), currentUser(authentication));
        requireManager(member);
        requireSchedulable(plan);
        List<Task> tasks = taskRepository.findByWeeklyPlanIdOrderByBacklogPositionAscCreatedAtAsc(planId);
        Set<Long> current = tasks.stream().map(Task::getId).collect(java.util.stream.Collectors.toSet());
        if (request.getTaskIds().size() != tasks.size() || !current.equals(new HashSet<>(request.getTaskIds()))) {
            throw new IllegalArgumentException("Danh sach sap xep task trong ke hoach khong hop le");
        }
        Map<Long, Task> byId = tasks.stream().collect(java.util.stream.Collectors.toMap(Task::getId, item -> item));
        for (int i = 0; i < request.getTaskIds().size(); i++) byId.get(request.getTaskIds().get(i)).setBacklogPosition((long) i);
        return response(plan, member.getRole());
    }

    @Transactional
    public WeeklyPlanResponse removeTask(Long planId, Long taskId, boolean includeSubtasks, Authentication authentication) {
        WeeklyPlan plan = planRepository.findByIdForUpdate(planId).orElseThrow(() -> notFound("Khong tim thay ke hoach"));
        WorkspaceMember member = member(plan.getProject(), currentUser(authentication));
        requireManager(member);
        requireSchedulable(plan);
        Task root = task(taskId);
        if (root.getWeeklyPlan() == null || !Objects.equals(root.getWeeklyPlan().getId(), planId)) throw new IllegalArgumentException("Task khong thuoc ke hoach nay");
        List<Task> tasks = familyForMove(root, includeSubtasks);
        validateParentDestination(tasks, PlanningState.UNPLANNED, null);
        moveTasks(tasks, PlanningState.UNPLANNED, null);
        User actor = member.getUser();
        tasks.forEach(task -> taskChanges.changed(task, actor, "TASK_ARCHIVED", "weeklyPlan",
                plan.getName(), null, "Đưa công việc ra khỏi kế hoạch tuần", true));
        return response(plan, member.getRole());
    }

    @Transactional
    public WeeklyPlanResponse startPlan(Long planId, StartWeeklyPlanRequest request, Authentication authentication) {
        WeeklyPlan plan = planRepository.findByIdForUpdate(planId).orElseThrow(() -> notFound("Khong tim thay ke hoach"));
        WorkspaceMember member = member(plan.getProject(), currentUser(authentication));
        requireManager(member);
        requireDraft(plan);
        checkVersion(plan, request.getVersion());
        List<Task> tasks = taskRepository.findByWeeklyPlanIdOrderByBacklogPositionAscCreatedAtAsc(planId);
        if (tasks.isEmpty()) throw new IllegalArgumentException("Ke hoach phai co it nhat mot task truoc khi bat dau");
        if (isOverloaded(plan, tasks) && !request.isConfirmOverload()) {
            throw new IllegalArgumentException("Khoi luong vuot suc chua. Can xac nhan qua tai de bat dau");
        }
        projectRepository.findByIdForUpdate(plan.getProject().getId()).orElseThrow(() -> notFound("Khong tim thay du an"));
        planRepository.findActiveByProjectIdForUpdate(plan.getProject().getId()).ifPresent(active -> {
            if (!Objects.equals(active.getId(), planId)) throw new IllegalArgumentException("Du an da co mot ke hoach dang hoat dong");
        });
        plan.changeStatus(WeeklyPlanStatus.ACTIVE);
        plan.setStartedAt(LocalDateTime.now());
        plan.setStartedBy(member.getUser());
        moveTasks(tasks, PlanningState.ACTIVE, plan);
        realtimeService.publish(plan.getProject().getId(), null, "WEEKLY_PLAN_UPDATED", plan.getVersion(), java.util.Map.of("planId", plan.getId(), "status", "ACTIVE"));
        memberRepository.findByWorkspaceIdOrderByJoinedAtAsc(plan.getProject().getWorkspace().getId()).stream().map(WorkspaceMember::getUser).filter(user -> !Objects.equals(user.getId(), member.getUser().getId())).forEach(user -> notificationService.createAndSendDetailed(user, "Kế hoạch tuần đã bắt đầu", plan.getName(), NotificationType.WEEKLY_PLAN_STARTED, plan.getId(), plan.getProject().getId(), null, "plan-start:" + plan.getId() + ":" + user.getId()));
        return response(plan, member.getRole());
    }

    @Transactional(readOnly = true)
    public WeeklyPlanCompletionResponse completionPreview(Long planId, Authentication authentication) {
        WeeklyPlan plan = plan(planId);
        WorkspaceMember member = member(plan.getProject(), currentUser(authentication));
        return completion(plan, member.getRole(), false);
    }

    @Transactional
    public WeeklyPlanCompletionResponse completePlan(Long planId, CompleteWeeklyPlanRequest request, Authentication authentication) {
        WeeklyPlan plan = planRepository.findByIdForUpdate(planId).orElseThrow(() -> notFound("Khong tim thay ke hoach"));
        WorkspaceMember member = member(plan.getProject(), currentUser(authentication));
        requireManager(member);
        if (plan.getStatus() != WeeklyPlanStatus.ACTIVE) throw new IllegalArgumentException("Chi ke hoach dang hoat dong moi duoc hoan thanh");
        checkVersion(plan, request.getVersion());
        List<Task> all = taskRepository.findByWeeklyPlanIdOrderByBacklogPositionAscCreatedAtAsc(planId);
        List<Task> incomplete = all.stream().filter(task -> !isDone(task)).toList();
        int totalBefore = all.size();
        int completedBefore = totalBefore - incomplete.size();
        BigDecimal plannedBefore = all.stream().map(Task::getEstimatedEffort).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal completedEffortBefore = all.stream().filter(this::isDone).map(Task::getActualEffort).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<TaskResponse> incompleteBefore = incomplete.stream().map(task -> TaskResponse.from(task, member.getRole())).toList();
        List<Task> toCarry = includeWholeFamilies(all, incomplete);
        saveCompletionSnapshots(plan, all);
        if (!toCarry.isEmpty()) handleIncomplete(plan, toCarry, request, member.getUser());
        all.stream().filter(task -> !toCarry.contains(task)).forEach(task -> {
            task.setPlanningState(PlanningState.COMPLETED);
            task.setBacklogPosition(null);
        });
        plan.changeStatus(WeeklyPlanStatus.COMPLETED);
        plan.setCompletedAt(LocalDateTime.now());
        plan.setCompletedBy(member.getUser());
        notifyPlanMembers(plan, all, member.getUser(), NotificationType.WEEKLY_PLAN_COMPLETED, "Ke hoach tuan da ket thuc");
        realtimeService.publish(plan.getProject().getId(), null, "WEEKLY_PLAN_UPDATED", plan.getVersion(), java.util.Map.of("planId", plan.getId(), "status", "COMPLETED"));
        return WeeklyPlanCompletionResponse.builder().totalTasks(totalBefore).completedTasks(completedBefore)
                .incompleteTasks(incompleteBefore.size()).incompleteTaskList(incompleteBefore)
                .completionRate(totalBefore == 0 ? 0 : completedBefore * 100.0 / totalBefore)
                .plannedEffort(plannedBefore).completedEffort(completedEffortBefore).plan(response(plan, member.getRole())).build();
    }

    private void handleIncomplete(WeeklyPlan source, List<Task> tasks, CompleteWeeklyPlanRequest request, User actor) {
        if (request.getAction() == IncompleteTaskAction.KEEP_IN_PLAN) {
            tasks.forEach(task -> { task.setPlanningState(PlanningState.COMPLETED); task.setBacklogPosition(null); });
            return;
        }
        if (request.getAction() == IncompleteTaskAction.MOVE_TO_UNPLANNED) {
            moveTasks(tasks, PlanningState.UNPLANNED, null);
            return;
        }
        WeeklyPlan target;
        if (request.getAction() == IncompleteTaskAction.MOVE_TO_PLAN) {
            if (request.getTargetPlanId() == null) throw new IllegalArgumentException("Phai chon ke hoach nhap dich");
            target = planRepository.findByIdForUpdate(request.getTargetPlanId()).orElseThrow(() -> notFound("Khong tim thay ke hoach dich"));
            if (!Objects.equals(target.getProject().getId(), source.getProject().getId()) || target.getStatus() != WeeklyPlanStatus.DRAFT) {
                throw new IllegalArgumentException("Ke hoach dich phai la ke hoach nhap cung du an");
            }
        } else {
            validateDates(request.getNextPlanStartDate(), request.getNextPlanEndDate());
            if (request.getNextPlanName() == null || request.getNextPlanName().isBlank()) throw new IllegalArgumentException("Ten ke hoach tiep theo khong duoc rong");
            target = planRepository.save(WeeklyPlan.builder().project(source.getProject()).name(request.getNextPlanName().trim())
                    .goal(normalize(request.getNextPlanGoal())).startDate(request.getNextPlanStartDate()).endDate(request.getNextPlanEndDate())
                    .status(WeeklyPlanStatus.DRAFT).createdBy(actor).build());
        }
        moveTasks(tasks, PlanningState.PLANNED, target);
    }

    private WeeklyPlanCompletionResponse completion(WeeklyPlan plan, WorkspaceRole role, boolean completed) {
        List<Task> tasks = taskRepository.findByWeeklyPlanIdOrderByBacklogPositionAscCreatedAtAsc(plan.getId());
        List<TaskResponse> incomplete = tasks.stream().filter(task -> !isDone(task)).map(task -> TaskResponse.from(task, role)).toList();
        BigDecimal plannedEffort = tasks.stream().map(Task::getEstimatedEffort).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal completedEffort = tasks.stream().filter(this::isDone).map(Task::getActualEffort).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        return WeeklyPlanCompletionResponse.builder().totalTasks(tasks.size()).completedTasks(tasks.size() - incomplete.size())
                .incompleteTasks(incomplete.size()).incompleteTaskList(incomplete)
                .completionRate(tasks.isEmpty() ? 0 : (tasks.size() - incomplete.size()) * 100.0 / tasks.size())
                .plannedEffort(plannedEffort).completedEffort(completedEffort)
                .plan(completed ? response(plan, role) : null).build();
    }

    @Transactional
    public WeeklyPlanResponse clonePlan(Long planId, Authentication authentication) {
        WeeklyPlan source = plan(planId);
        User actor = currentUser(authentication);
        WorkspaceMember member = member(source.getProject(), actor);
        requireManager(member);
        WeeklyPlan copy = planRepository.save(WeeklyPlan.builder().project(source.getProject())
                .name("Ban sao - " + source.getName()).goal(source.getGoal()).description(source.getDescription())
                .capacity(source.getCapacity()).estimateUnit(source.getEstimateUnit()).startDate(source.getStartDate())
                .endDate(source.getEndDate()).status(WeeklyPlanStatus.DRAFT).createdBy(actor).build());
        return response(copy, member.getRole());
    }

    @Transactional
    public WeeklyPlanResponse cancelPlan(Long planId, Authentication authentication) {
        WeeklyPlan plan = planRepository.findByIdForUpdate(planId).orElseThrow(() -> notFound("Khong tim thay ke hoach"));
        User actor = currentUser(authentication);
        WorkspaceMember member = member(plan.getProject(), actor);
        requireManager(member);
        if (plan.getStatus() == WeeklyPlanStatus.COMPLETED || plan.getStatus() == WeeklyPlanStatus.CANCELLED) {
            throw new IllegalArgumentException("Ke hoach da ket thuc hoac da huy");
        }
        List<Task> tasks = taskRepository.findByWeeklyPlanIdOrderByBacklogPositionAscCreatedAtAsc(planId);
        moveTasks(tasks, PlanningState.UNPLANNED, null);
        plan.changeStatus(WeeklyPlanStatus.CANCELLED);
        plan.setCancelledAt(LocalDateTime.now());
        plan.setCancelledBy(actor);
        notifyPlanMembers(plan, tasks, actor, NotificationType.WEEKLY_PLAN_CANCELLED, "Ke hoach tuan da bi huy");
        return response(plan, member.getRole());
    }

    private void saveCompletionSnapshots(WeeklyPlan plan, List<Task> tasks) {
        if (snapshotRepository.existsByWeeklyPlanId(plan.getId())) return;
        long order = 0;
        for (Task task : tasks) snapshotRepository.save(WeeklyPlanTaskSnapshot.from(plan, task, isDone(task), order++));
    }

    private void notifyPlanMembers(WeeklyPlan plan, List<Task> tasks, User actor, NotificationType type, String title) {
        tasks.stream().map(Task::getAssignedTo).filter(Objects::nonNull).filter(user -> !Objects.equals(user.getId(), actor.getId()))
                .collect(java.util.stream.Collectors.toMap(User::getId, user -> user, (a, b) -> a)).values()
                .forEach(user -> notificationService.createAndSendDetailed(user, title, plan.getName(), type, plan.getId(), plan.getProject().getId(), null, type.name() + ":" + plan.getId() + ":" + user.getId()));
    }

    private boolean isOverloaded(WeeklyPlan plan, List<Task> tasks) {
        if (plan.getCapacity() == null || plan.getCapacity().signum() <= 0) return false;
        return tasks.stream().map(Task::getEstimatedEffort).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add).compareTo(plan.getCapacity()) > 0;
    }

    private List<Task> includeWholeFamilies(List<Task> all, List<Task> incomplete) {
        Set<Task> result = new LinkedHashSet<>(incomplete);
        for (Task task : incomplete) {
            Task root = task.getParentTask() == null ? task : task.getParentTask();
            result.add(root);
            all.stream().filter(item -> item.getParentTask() != null && Objects.equals(item.getParentTask().getId(), root.getId())).forEach(result::add);
        }
        return new ArrayList<>(result);
    }

    private List<Task> familyForMove(Task root, boolean includeSubtasks) {
        return expandFamilies(List.of(root), includeSubtasks);
    }

    private List<Task> expandFamilies(List<Task> roots, boolean includeSubtasks) {
        Set<Task> result = new LinkedHashSet<>(roots);
        for (Task root : roots) {
            List<Task> children = taskRepository.findByParentTaskIdOrderByCreatedAtAsc(root.getId());
            boolean allSelected = children.stream().allMatch(result::contains);
            if (!children.isEmpty() && !includeSubtasks && !allSelected) {
                throw new IllegalArgumentException("Task cha co task con. Hay xac nhan di chuyen toan bo task con");
            }
            if (includeSubtasks) result.addAll(children);
        }
        return new ArrayList<>(result);
    }

    private void validateParentDestination(List<Task> moving, PlanningState state, WeeklyPlan plan) {
        Set<Long> movingIds = moving.stream().map(Task::getId).collect(java.util.stream.Collectors.toSet());
        for (Task task : moving) {
            Task parent = task.getParentTask();
            if (parent == null || movingIds.contains(parent.getId())) continue;
            boolean sameState = parent.getPlanningState() == state;
            boolean samePlan = Objects.equals(parent.getWeeklyPlan() == null ? null : parent.getWeeklyPlan().getId(), plan == null ? null : plan.getId());
            if (!sameState || !samePlan) throw new IllegalArgumentException("Task con khong duoc nam khac noi lap ke hoach voi task cha");
        }
    }

    private void moveTasks(List<Task> tasks, PlanningState state, WeeklyPlan plan) {
        long next = state == PlanningState.UNPLANNED && !tasks.isEmpty() ? nextUnplannedPosition(tasks.get(0).getProject().getId()) : 0;
        for (Task task : tasks) {
            task.setPlanningState(state);
            task.setWeeklyPlan(plan);
            if (state != PlanningState.ACTIVE || task.getBoardColumn() == null) {
                task.setBoardColumn(boardService.getDefaultColumn(task.getProject(), StatusGroup.TODO));
                task.setStatus(TaskStatus.TODO);
            }
            task.setBacklogPosition(state == PlanningState.UNPLANNED || state == PlanningState.PLANNED ? next++ : null);
        }
    }

    private long nextUnplannedPosition(Long projectId) {
        return taskRepository.findByProjectIdAndPlanningStateOrderByBacklogPositionAscCreatedAtAsc(projectId, PlanningState.UNPLANNED).size();
    }

    private boolean isDone(Task task) {
        return task.getBoardColumn() != null && task.getBoardColumn().getStatusGroup() == StatusGroup.DONE;
    }

    private WeeklyPlanResponse response(WeeklyPlan plan, WorkspaceRole role) {
        List<Task> entities = taskRepository.findByWeeklyPlanIdOrderByBacklogPositionAscCreatedAtAsc(plan.getId());
        List<TaskResponse> tasks = entities.stream().map(task -> TaskResponse.from(task, role)).toList();
        List<PlanTaskSnapshotResponse> snapshots = snapshotRepository.findByWeeklyPlanIdOrderBySortOrderAsc(plan.getId()).stream().map(PlanTaskSnapshotResponse::from).toList();
        return WeeklyPlanResponse.from(plan, tasks, workloads(plan, entities), snapshots);
    }

    private List<MemberWorkloadResponse> workloads(WeeklyPlan plan, List<Task> tasks) {
        Map<Long, List<Task>> assigned = new LinkedHashMap<>();
        List<Task> unassigned = new ArrayList<>();
        for (Task task : tasks) {
            if (task.getAssignedTo() == null) unassigned.add(task);
            else assigned.computeIfAbsent(task.getAssignedTo().getId(), ignored -> new ArrayList<>()).add(task);
        }
        int assigneeCount = Math.max(1, assigned.size());
        BigDecimal share = plan.getCapacity() == null ? BigDecimal.ZERO : plan.getCapacity().divide(BigDecimal.valueOf(assigneeCount), 2, RoundingMode.HALF_UP);
        List<MemberWorkloadResponse> result = new ArrayList<>();
        assigned.values().forEach(memberTasks -> {
            BigDecimal effort = memberTasks.stream().map(Task::getEstimatedEffort).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            double percent = share.signum() == 0 ? 0 : effort.multiply(BigDecimal.valueOf(100)).divide(share, 2, RoundingMode.HALF_UP).doubleValue();
            result.add(MemberWorkloadResponse.builder().member(com.teamspace.teamspace.workspace.dto.WorkspaceUserSummary.from(memberTasks.get(0).getAssignedTo()))
                    .label(memberTasks.get(0).getAssignedTo().getFullName()).taskCount(memberTasks.size()).allocatedEffort(effort)
                    .capacityShare(share).utilizationPercent(percent).overloaded(percent > 100).build());
        });
        if (!unassigned.isEmpty()) {
            BigDecimal effort = unassigned.stream().map(Task::getEstimatedEffort).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            result.add(MemberWorkloadResponse.builder().label("Chua giao").taskCount(unassigned.size()).allocatedEffort(effort)
                    .capacityShare(BigDecimal.ZERO).utilizationPercent(0).overloaded(false).build());
        }
        return result;
    }

    private WorkspaceMember requireCanMove(Task task, User user) {
        WorkspaceMember member = member(task.getProject(), user);
        if (!isManager(member) && (task.getAssignedTo() == null || !Objects.equals(task.getAssignedTo().getId(), user.getId()))) {
            throw new ForbiddenException("Ban khong co quyen di chuyen task nay");
        }
        return member;
    }

    private void requireManager(WorkspaceMember member) {
        if (!isManager(member)) throw new ForbiddenException("Chi OWNER hoac LEADER duoc quan ly ke hoach tuan");
    }

    private boolean isManager(WorkspaceMember member) {
        return member.getRole() == WorkspaceRole.OWNER || member.getRole() == WorkspaceRole.LEADER;
    }

    private void requireDraft(WeeklyPlan plan) {
        if (plan.getStatus() != WeeklyPlanStatus.DRAFT) throw new IllegalArgumentException("Chi ke hoach nhap moi duoc sua hoac xoa");
    }

    private void requireSchedulable(WeeklyPlan plan) {
        if (plan.getStatus() != WeeklyPlanStatus.DRAFT && plan.getStatus() != WeeklyPlanStatus.ACTIVE) {
            throw new IllegalArgumentException("Chi ke hoach nhap hoac dang hoat dong moi duoc thay doi cong viec");
        }
    }

    private void checkVersion(WeeklyPlan plan, Long version) {
        if (version != null && !Objects.equals(version, plan.getVersion())) throw new IllegalArgumentException("Ke hoach da duoc cap nhat. Vui long tai lai du lieu");
    }

    private void validateDates(LocalDate start, LocalDate end) {
        if (start == null || end == null || !end.isAfter(start)) throw new IllegalArgumentException("Ngay ket thuc phai sau ngay bat dau");
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Project project(Long id) {
        return projectRepository.findById(id).orElseThrow(() -> notFound("Khong tim thay du an"));
    }

    private WeeklyPlan plan(Long id) {
        return planRepository.findById(id).orElseThrow(() -> notFound("Khong tim thay ke hoach"));
    }

    private Task task(Long id) {
        return taskRepository.findById(id).orElseThrow(() -> notFound("Khong tim thay task"));
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) throw new UnauthorizedException("Ban chua dang nhap");
        return userRepository.findByEmail(authentication.getName()).orElseThrow(() -> new UnauthorizedException("Khong tim thay user hien tai"));
    }

    private WorkspaceMember member(Project project, User user) {
        return memberRepository.findByWorkspaceIdAndUserId(project.getWorkspace().getId(), user.getId())
                .orElseThrow(() -> new ForbiddenException("Ban khong thuoc du an nay"));
    }

    private ResourceNotFoundException notFound(String message) {
        return new ResourceNotFoundException(message);
    }
}
