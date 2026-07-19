package com.teamspace.teamspace.task.service;

import java.util.List;
import java.util.HashSet;
import java.util.Objects;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamspace.teamspace.activity.enums.ActivityAction;
import com.teamspace.teamspace.activity.service.ActivityLogService;
import com.teamspace.teamspace.exception.ForbiddenException;
import com.teamspace.teamspace.exception.ResourceNotFoundException;
import com.teamspace.teamspace.exception.UnauthorizedException;
import com.teamspace.teamspace.notification.enums.NotificationType;
import com.teamspace.teamspace.notification.service.NotificationService;
import com.teamspace.teamspace.project.entity.Project;
import com.teamspace.teamspace.project.repository.ProjectRepository;
import com.teamspace.teamspace.task.dto.CreateTaskCommentRequest;
import com.teamspace.teamspace.task.dto.CreateChecklistItemRequest;
import com.teamspace.teamspace.task.dto.ChecklistItemResponse;
import com.teamspace.teamspace.task.dto.CreateTaskRequest;
import com.teamspace.teamspace.task.dto.TaskCommentResponse;
import com.teamspace.teamspace.task.dto.TaskResponse;
import com.teamspace.teamspace.task.dto.UpdateTaskRequest;
import com.teamspace.teamspace.task.dto.UpdateTaskStatusRequest;
import com.teamspace.teamspace.task.dto.UpdateChecklistItemRequest;
import com.teamspace.teamspace.task.dto.ReorderChecklistRequest;
import com.teamspace.teamspace.task.entity.BoardColumn;
import com.teamspace.teamspace.task.entity.Task;
import com.teamspace.teamspace.task.entity.TaskComment;
import com.teamspace.teamspace.task.entity.TaskChecklistItem;
import com.teamspace.teamspace.task.enums.StatusGroup;
import com.teamspace.teamspace.task.enums.SubtaskDeleteAction;
import com.teamspace.teamspace.task.enums.TaskPriority;
import com.teamspace.teamspace.task.enums.TaskStatus;
import com.teamspace.teamspace.task.repository.TaskCommentRepository;
import com.teamspace.teamspace.task.repository.TaskChecklistItemRepository;
import com.teamspace.teamspace.task.repository.TaskRepository;
import com.teamspace.teamspace.user.entity.User;
import com.teamspace.teamspace.user.repository.UserRepository;
import com.teamspace.teamspace.workspace.entity.WorkspaceMember;
import com.teamspace.teamspace.workspace.enums.WorkspaceRole;
import com.teamspace.teamspace.workspace.repository.WorkspaceMemberRepository;
import com.teamspace.teamspace.planning.enums.PlanningState;
import com.teamspace.teamspace.workcategory.service.WorkCategoryService;
import com.teamspace.teamspace.realtime.service.TaskChangeCoordinator;
import com.teamspace.teamspace.task.repository.TaskWatcherRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskCommentRepository taskCommentRepository;
    private final TaskChecklistItemRepository taskChecklistItemRepository;
    private final ProjectRepository projectRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ActivityLogService activityLogService;
    private final BoardService boardService;
    private final WorkCategoryService workCategoryService;
    private final TaskChangeCoordinator taskChanges;
    private final TaskWatcherService watcherService;
    private final TaskWatcherRepository taskWatcherRepository;
    private final TaskDeletionService taskDeletionService;

    @Transactional
    public TaskResponse createTask(Long projectId, CreateTaskRequest request, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        Project project = getProjectOrThrow(projectId);
        WorkspaceMember currentMember = getCurrentMemberOrThrow(project.getWorkspace().getId(), currentUser.getId());

        User assignedTo = resolveAssignee(project.getWorkspace().getId(), request.getAssignedToUserId());
        if (!canManageTask(currentMember) && assignedTo != null && !Objects.equals(assignedTo.getId(), currentUser.getId())) {
            throw new ForbiddenException("Thanh vien khong duoc giao task cho nguoi khac");
        }
        Task parentTask = resolveParentTask(project, request.getParentTaskId(), null);

        TaskStatus requestedStatus = request.getStatus() == null ? TaskStatus.TODO : request.getStatus();
        var workCategory = workCategoryService.resolve(project, request.getWorkCategoryId(), request.getType());
        BoardColumn boardColumn = resolveCompatibleColumn(project, request.getBoardColumnId(), requestedStatus);
        Task task = Task.builder()
                .project(project)
                .title(request.getTitle().trim())
                .description(normalizeText(request.getDescription()))
                .assignedTo(assignedTo)
                .parentTask(parentTask)
                .boardColumn(boardColumn)
                .boardPosition(boardService.nextTaskPosition(boardColumn))
                .planningState(parentTask == null ? PlanningState.ACTIVE : parentTask.getPlanningState())
                .weeklyPlan(parentTask == null ? null : parentTask.getWeeklyPlan())
                .status(statusForGroup(boardColumn.getStatusGroup()))
                .priority(request.getPriority() == null ? TaskPriority.MEDIUM : request.getPriority())
                .type(normalizeTaskType(request.getType()))
                .workCategory(workCategory)
                .reviewRequired(request.getReviewRequired() == null || request.getReviewRequired())
                .startDate(request.getStartDate())
                .dueDate(request.getDueDate())
                .labels(normalizeText(request.getLabels()))
                .estimatedEffort(request.getEstimatedEffort())
                .actualEffort(request.getActualEffort())
                .createdBy(currentUser)
                .build();

        boardService.validateBoardTransition(task, boardColumn);

        Task savedTask = taskRepository.save(task);
        watcherService.autoFollow(savedTask, currentUser);
        watcherService.autoFollow(savedTask, assignedTo);
        taskChanges.changed(savedTask, currentUser, "TASK_CREATED", null, null, savedTask.getTitle(), currentUser.getFullName() + " tạo công việc " + savedTask.getTitle(), false);
        if (parentTask != null) {
            taskChanges.changed(savedTask, currentUser, "SUBTASK_ADDED", "parentTask", null,
                    parentTask.getTitle(), currentUser.getFullName() + " thêm công việc con vào " + parentTask.getTitle(), false);
        }
        activityLogService.log(
                project,
                currentUser,
                ActivityAction.TASK_CREATED,
                "TASK",
                savedTask.getId(),
                currentUser.getFullName() + " created task: " + savedTask.getTitle()
        );
        notifyTaskAssigned(savedTask, currentUser);
        return TaskResponse.from(savedTask, currentMember.getRole());
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getProjectTasks(Long projectId, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        Project project = getProjectOrThrow(projectId);
        WorkspaceMember currentMember = getCurrentMemberOrThrow(project.getWorkspace().getId(), currentUser.getId());

        boardService.ensureProjectBoard(project);
        return taskRepository.findByProjectIdAndPlanningStateOrderByBacklogPositionAscCreatedAtAsc(projectId, PlanningState.ACTIVE)
                .stream()
                .map(task -> TaskResponse.from(task, currentMember.getRole()))
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse getTaskDetail(Long taskId, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        Task task = getTaskOrThrow(taskId);
        WorkspaceMember currentMember = getCurrentMemberOrThrow(
                task.getProject().getWorkspace().getId(),
                currentUser.getId()
        );

        return TaskResponse.from(task, currentMember.getRole());
    }

    @Transactional
    public TaskResponse updateTask(Long taskId, UpdateTaskRequest request, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        Task task = getTaskOrThrow(taskId);
        WorkspaceMember currentMember = getCurrentMemberOrThrow(
                task.getProject().getWorkspace().getId(),
                currentUser.getId()
        );
        requireCanUpdateTask(currentMember, task, currentUser);
        if (request.getVersion() != null && !Objects.equals(request.getVersion(), task.getVersion())) {
            throw new IllegalArgumentException("Task da duoc cap nhat boi nguoi khac. Vui long tai lai du lieu");
        }

        User previousAssignee = task.getAssignedTo();
        String oldTitle = task.getTitle();
        String oldDescription = task.getDescription();
        String oldPriority = String.valueOf(task.getPriority());
        String oldDueDate = String.valueOf(task.getDueDate());
        String oldEstimatedEffort = String.valueOf(task.getEstimatedEffort());
        String oldActualEffort = String.valueOf(task.getActualEffort());
        String oldStartDate = String.valueOf(task.getStartDate());
        String oldLabels = task.getLabels();
        String oldCategory = task.getWorkCategory() == null ? task.getType() : task.getWorkCategory().getName();
        BoardColumn previousColumn = task.getBoardColumn();
        String oldColumn = previousColumn == null ? null : previousColumn.getLabel();
        String oldParent = task.getParentTask() == null ? null : task.getParentTask().getTitle();
        User assignedTo = resolveAssignee(task.getProject().getWorkspace().getId(), request.getAssignedToUserId());
        if (!canManageTask(currentMember) && !sameUser(task.getAssignedTo(), assignedTo)) {
            throw new ForbiddenException("Thanh vien khong duoc giao cong viec cho nguoi khac");
        }

        task.setTitle(request.getTitle().trim());
        task.setDescription(normalizeText(request.getDescription()));
        task.setAssignedTo(assignedTo);
        task.setParentTask(resolveParentTask(task.getProject(), request.getParentTaskId(), task));
        BoardColumn targetColumn = resolveCompatibleColumn(task.getProject(), request.getBoardColumnId(), request.getStatus());
        task.setStatus(statusForGroup(targetColumn.getStatusGroup()));
        task.setPriority(request.getPriority());
        task.setType(normalizeTaskType(request.getType()));
        task.setWorkCategory(workCategoryService.resolve(task.getProject(), request.getWorkCategoryId(), request.getType()));
        task.setReviewRequired(request.isReviewRequired());
        task.setStartDate(request.getStartDate());
        task.setDueDate(request.getDueDate());
        task.setLabels(normalizeText(request.getLabels()));
        task.setEstimatedEffort(request.getEstimatedEffort());
        task.setActualEffort(request.getActualEffort());

        boardService.validateBoardTransition(task, targetColumn);
        task.setBoardColumn(targetColumn);
        if (!sameColumn(previousColumn, targetColumn)) {
            task.setBoardPosition(boardService.nextTaskPosition(targetColumn));
        }

        logChange(task, currentUser, "title", oldTitle, task.getTitle());
        logChange(task, currentUser, "description", oldDescription, task.getDescription());
        logChange(task, currentUser, "priority", oldPriority, String.valueOf(task.getPriority()));
        logChange(task, currentUser, "dueDate", oldDueDate, String.valueOf(task.getDueDate()));
        logChange(task, currentUser, "estimatedEffort", oldEstimatedEffort, String.valueOf(task.getEstimatedEffort()));
        logChange(task, currentUser, "actualEffort", oldActualEffort, String.valueOf(task.getActualEffort()));
        logChange(task, currentUser, "startDate", oldStartDate, String.valueOf(task.getStartDate()));
        logChange(task, currentUser, "labels", oldLabels, task.getLabels());
        logChange(task, currentUser, "workCategory", oldCategory, task.getWorkCategory().getName());
        logChange(task, currentUser, "column", oldColumn, targetColumn.getLabel());
        logChange(task, currentUser, "parentTask", oldParent,
                task.getParentTask() == null ? null : task.getParentTask().getTitle());

        if (!sameUser(previousAssignee, assignedTo)) {
            logChange(task, currentUser, "assignedTo", previousAssignee == null ? null : previousAssignee.getFullName(), assignedTo == null ? null : assignedTo.getFullName());
            if (previousAssignee != null) notificationService.createAndSendDetailed(previousAssignee, "Đã gỡ khỏi công việc", currentUser.getFullName() + " đã gỡ bạn khỏi " + task.getTitle(), NotificationType.TASK_UNASSIGNED, task.getId(), task.getProject().getId(), task.getId(), null);
            watcherService.autoFollow(task, assignedTo);
            notifyTaskAssigned(task, currentUser);
        }

        activityLogService.log(
                task.getProject(),
                currentUser,
                ActivityAction.TASK_UPDATED,
                "TASK",
                task.getId(),
                currentUser.getFullName() + " updated task: " + task.getTitle()
        );

        return TaskResponse.from(task, currentMember.getRole());
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getSubtasks(Long taskId, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        Task parent = getTaskOrThrow(taskId);
        WorkspaceMember member = getCurrentMemberOrThrow(parent.getProject().getWorkspace().getId(), currentUser.getId());
        return taskRepository.findByParentTaskIdOrderByCreatedAtAsc(taskId).stream()
                .map(task -> TaskResponse.from(task, member.getRole()))
                .toList();
    }

    @Transactional
    public void deleteTask(Long taskId, SubtaskDeleteAction subtaskAction, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        Task task = getTaskOrThrow(taskId);
        WorkspaceMember member = getCurrentMemberOrThrow(task.getProject().getWorkspace().getId(), currentUser.getId());
        if (!canManageTask(member) && !Objects.equals(task.getCreatedBy().getId(), currentUser.getId())) {
            throw new ForbiddenException("Ban khong co quyen xoa task nay");
        }

        List<Task> subtasks = taskRepository.findByParentTaskIdOrderByCreatedAtAsc(taskId);
        if (!subtasks.isEmpty() && subtaskAction == null) {
            throw new IllegalArgumentException("Phai chon xoa task con hoac chuyen thanh task doc lap");
        }
        if (subtaskAction == SubtaskDeleteAction.DELETE) {
            subtasks.forEach(taskDeletionService::cleanup);
            taskRepository.deleteAll(subtasks);
            taskRepository.flush();
        } else if (subtaskAction == SubtaskDeleteAction.DETACH) {
            subtasks.forEach(subtask -> {
                subtask.setParentTask(null);
                taskChanges.changed(subtask, currentUser, "SUBTASK_DETACHED", "parentTask",
                        task.getTitle(), null, currentUser.getFullName() + " tách công việc con khỏi " + task.getTitle(), true);
            });
            taskRepository.saveAll(subtasks);
            taskRepository.flush();
        }
        taskChanges.event(task, "TASK_DELETED");
        taskDeletionService.cleanup(task);
        taskRepository.delete(task);
    }

    @Transactional(readOnly = true)
    public List<ChecklistItemResponse> getChecklist(Long taskId, Authentication authentication) {
        requireTaskMember(getTaskOrThrow(taskId), authentication);
        return taskChecklistItemRepository.findByTaskIdOrderByPositionAscIdAsc(taskId).stream()
                .map(ChecklistItemResponse::from)
                .toList();
    }

    @Transactional
    public ChecklistItemResponse createChecklistItem(Long taskId, CreateChecklistItemRequest request, Authentication authentication) {
        Task task = getTaskOrThrow(taskId);
        User actor = getCurrentUser(authentication);
        requireTaskMember(task, authentication);
        TaskChecklistItem item = TaskChecklistItem.builder()
                .task(task)
                .content(request.getContent().trim())
                .completed(false)
                .position((int) taskChecklistItemRepository.countByTaskId(taskId))
                .build();
        ChecklistItemResponse response = ChecklistItemResponse.from(taskChecklistItemRepository.save(item));
        taskChanges.changed(task, actor, "CHECKLIST_UPDATED", "checklist", null, item.getContent(), actor.getFullName() + " thêm mục checklist " + item.getContent(), false);
        return response;
    }

    @Transactional
    public ChecklistItemResponse updateChecklistItem(Long taskId, Long itemId, UpdateChecklistItemRequest request, Authentication authentication) {
        Task task = getTaskOrThrow(taskId);
        User actor = getCurrentUser(authentication);
        requireTaskMember(task, authentication);
        TaskChecklistItem item = getChecklistItem(taskId, itemId);
        String oldValue = checklistValue(item);
        item.setContent(request.getContent().trim());
        item.setCompleted(request.isCompleted());
        String action = request.isCompleted() ? "CHECKLIST_COMPLETED" : "CHECKLIST_UPDATED";
        taskChanges.changed(task, actor, action, "checklist", oldValue, checklistValue(item),
                actor.getFullName() + (request.isCompleted() ? " hoàn thành checklist " : " cập nhật checklist ") + item.getContent(), false);
        return ChecklistItemResponse.from(item);
    }

    @Transactional
    public void deleteChecklistItem(Long taskId, Long itemId, Authentication authentication) {
        Task task = getTaskOrThrow(taskId);
        User actor = getCurrentUser(authentication);
        requireTaskMember(task, authentication);
        TaskChecklistItem item = getChecklistItem(taskId, itemId);
        String deletedContent = item.getContent();
        taskChecklistItemRepository.delete(item);
        List<TaskChecklistItem> remaining = taskChecklistItemRepository.findByTaskIdOrderByPositionAscIdAsc(taskId);
        remaining.removeIf(current -> Objects.equals(current.getId(), itemId));
        for (int index = 0; index < remaining.size(); index++) remaining.get(index).setPosition(index);
        taskChanges.changed(task, actor, "CHECKLIST_UPDATED", "checklist", deletedContent, null, actor.getFullName() + " xóa mục checklist " + deletedContent, false);
    }

    @Transactional
    public List<ChecklistItemResponse> reorderChecklist(Long taskId, ReorderChecklistRequest request, Authentication authentication) {
        Task task = getTaskOrThrow(taskId);
        requireTaskMember(task, authentication);
        List<TaskChecklistItem> items = taskChecklistItemRepository.findByTaskIdOrderByPositionAscIdAsc(taskId);
        List<Long> requestedIds = request.getItemIds();
        if (requestedIds.size() != items.size()
                || new HashSet<>(requestedIds).size() != requestedIds.size()
                || !new HashSet<>(requestedIds).equals(items.stream().map(TaskChecklistItem::getId).collect(java.util.stream.Collectors.toSet()))) {
            throw new IllegalArgumentException("Danh sach sap xep checklist khong hop le");
        }
        var byId = items.stream().collect(java.util.stream.Collectors.toMap(TaskChecklistItem::getId, item -> item));
        for (int index = 0; index < requestedIds.size(); index++) byId.get(requestedIds.get(index)).setPosition(index);
        return requestedIds.stream().map(byId::get).map(ChecklistItemResponse::from).toList();
    }

    @Transactional
    public TaskResponse updateTaskStatus(
            Long taskId,
            UpdateTaskStatusRequest request,
            Authentication authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        Task task = getTaskOrThrow(taskId);
        if (task.getPlanningState() != PlanningState.ACTIVE) {
            throw new IllegalArgumentException("Chi task dang hien thi tren Board moi duoc doi trang thai");
        }
        WorkspaceMember currentMember = getCurrentMemberOrThrow(
                task.getProject().getWorkspace().getId(),
                currentUser.getId()
        );
        requireCanUpdateTask(currentMember, task, currentUser);

        String oldColumn = task.getBoardColumn() == null ? null : task.getBoardColumn().getLabel();
        task.setStatus(request.getStatus());
        var boardColumn = boardService.resolveColumn(task.getProject(), null, request.getStatus());
        boardService.validateBoardTransition(task, boardColumn);
        task.setBoardColumn(boardColumn);
        task.setBoardPosition(boardService.nextTaskPosition(boardColumn));
        taskChanges.changed(task, currentUser, "TASK_MOVED", "column", oldColumn, boardColumn.getLabel(), currentUser.getFullName() + " chuyển công việc từ " + oldColumn + " sang " + boardColumn.getLabel(), true);
        activityLogService.log(
                task.getProject(),
                currentUser,
                ActivityAction.TASK_STATUS_CHANGED,
                "TASK",
                task.getId(),
                currentUser.getFullName() + " changed task status to " + request.getStatus() + ": " + task.getTitle()
        );
        return TaskResponse.from(task, currentMember.getRole());
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getMyTasks(Authentication authentication) {
        User currentUser = getCurrentUser(authentication);

        return taskRepository.findByAssignedToIdOrderByDueDateAscCreatedAtDesc(currentUser.getId())
                .stream()
                .filter(task -> workspaceMemberRepository.existsByWorkspaceIdAndUserId(
                        task.getProject().getWorkspace().getId(),
                        currentUser.getId()
                ))
                .map(task -> {
                    WorkspaceMember member = getCurrentMemberOrThrow(
                            task.getProject().getWorkspace().getId(),
                            currentUser.getId()
                    );
                    return TaskResponse.from(task, member.getRole());
                })
                .toList();
    }

    @Transactional
    public TaskCommentResponse createComment(
            Long taskId,
            CreateTaskCommentRequest request,
            Authentication authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        Task task = getTaskOrThrow(taskId);
        getCurrentMemberOrThrow(task.getProject().getWorkspace().getId(), currentUser.getId());

        TaskComment comment = TaskComment.builder()
                .task(task)
                .author(currentUser)
                .content(request.getContent().trim())
                .build();

        var mentionIds = new HashSet<>(request.getMentionedUserIds() == null ? List.<Long>of() : request.getMentionedUserIds());
        var mentionedUsers = userRepository.findAllById(mentionIds);
        if (mentionedUsers.size() != mentionIds.size() || mentionedUsers.stream().anyMatch(user ->
                !workspaceMemberRepository.existsByWorkspaceIdAndUserId(task.getProject().getWorkspace().getId(), user.getId()))) {
            throw new IllegalArgumentException("Chi duoc mention thanh vien trong du an");
        }
        comment.getMentionedUsers().addAll(mentionedUsers);

        TaskComment savedComment = taskCommentRepository.save(comment);
        watcherService.autoFollow(task, currentUser);
        mentionedUsers.stream().filter(user -> !Objects.equals(user.getId(), currentUser.getId())).forEach(user ->
                notificationService.createAndSendDetailed(user, "Bạn được nhắc trong bình luận", currentUser.getFullName() + " đã nhắc bạn trong " + task.getTitle(), NotificationType.TASK_MENTION, task.getId(), task.getProject().getId(), task.getId(), "mention:" + savedComment.getId() + ":" + user.getId()));
        taskWatcherRepository.findByTaskIdOrderByCreatedAtAsc(taskId).stream().map(watcher -> watcher.getUser())
                .filter(user -> !Objects.equals(user.getId(), currentUser.getId()) && !mentionIds.contains(user.getId()))
                .forEach(user -> notificationService.createAndSendDetailed(user, "Bình luận mới", currentUser.getFullName() + " đã bình luận trong " + task.getTitle(), NotificationType.TASK_WATCHED_COMMENT, task.getId(), task.getProject().getId(), task.getId(), "comment:" + savedComment.getId() + ":" + user.getId()));
        taskChanges.event(task, "TASK_UPDATED");
        activityLogService.log(
                task.getProject(),
                currentUser,
                ActivityAction.TASK_COMMENTED,
                "TASK",
                task.getId(),
                currentUser.getFullName() + " commented on task: " + task.getTitle()
        );
        return TaskCommentResponse.from(savedComment);
    }

    @Transactional(readOnly = true)
    public List<TaskCommentResponse> getComments(Long taskId, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        Task task = getTaskOrThrow(taskId);
        getCurrentMemberOrThrow(task.getProject().getWorkspace().getId(), currentUser.getId());

        return taskCommentRepository.findByTaskIdOrderByCreatedAtAsc(taskId)
                .stream()
                .map(TaskCommentResponse::from)
                .toList();
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new UnauthorizedException("Ban chua dang nhap");
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UnauthorizedException("Khong tim thay user hien tai"));
    }

    private Project getProjectOrThrow(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay project"));
    }

    private Task getTaskOrThrow(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay task"));
    }

    private Task resolveParentTask(Project project, Long parentTaskId, Task currentTask) {
        if (parentTaskId == null) return null;
        Task parent = getTaskOrThrow(parentTaskId);
        if (!Objects.equals(parent.getProject().getId(), project.getId())) {
            throw new ForbiddenException("Task cha khong thuoc project nay");
        }
        if (currentTask != null && Objects.equals(parent.getId(), currentTask.getId())) {
            throw new IllegalArgumentException("Task khong the la task cha cua chinh no");
        }
        Task cursor = parent;
        while (cursor != null) {
            if (currentTask != null && Objects.equals(cursor.getId(), currentTask.getId())) {
                throw new IllegalArgumentException("Quan he task cha con khong duoc tao vong lap");
            }
            cursor = cursor.getParentTask();
        }
        if (parent.getParentTask() != null) {
            throw new IllegalArgumentException("Task con khong the co them task con");
        }
        if (currentTask != null && parentTaskId != null && taskRepository.countByParentTaskId(currentTask.getId()) > 0) {
            throw new IllegalArgumentException("Task dang co task con khong the chuyen thanh task con");
        }
        return parent;
    }

    private WorkspaceMember requireTaskMember(Task task, Authentication authentication) {
        User user = getCurrentUser(authentication);
        return getCurrentMemberOrThrow(task.getProject().getWorkspace().getId(), user.getId());
    }

    private TaskChecklistItem getChecklistItem(Long taskId, Long itemId) {
        TaskChecklistItem item = taskChecklistItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay muc checklist"));
        if (!Objects.equals(item.getTask().getId(), taskId)) {
            throw new ForbiddenException("Muc checklist khong thuoc task nay");
        }
        return item;
    }

    private WorkspaceMember getCurrentMemberOrThrow(Long workspaceId, Long userId) {
        return workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new ForbiddenException("Ban khong co quyen truy cap workspace nay"));
    }

    private User resolveAssignee(Long workspaceId, Long assignedToUserId) {
        if (assignedToUserId == null) {
            return null;
        }

        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, assignedToUserId)) {
            throw new ForbiddenException("Nguoi duoc giao cong viec phai thuoc khong gian lam viec");
        }

        return userRepository.findById(assignedToUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay user duoc giao task"));
    }

    private void requireOwnerOrLeader(WorkspaceMember member) {
        if (!canManageTask(member)) {
            throw new ForbiddenException("Chi OWNER hoac LEADER moi duoc thuc hien hanh dong nay");
        }
    }

    private void requireCanUpdateTask(WorkspaceMember member, Task task, User currentUser) {
        if (canManageTask(member)) {
            return;
        }

        if (task.getAssignedTo() == null || !Objects.equals(task.getAssignedTo().getId(), currentUser.getId())) {
            throw new ForbiddenException("Thanh vien chi duoc cap nhat cong viec duoc giao cho minh");
        }
    }

    private boolean canManageTask(WorkspaceMember member) {
        return member.getRole() == WorkspaceRole.OWNER || member.getRole() == WorkspaceRole.LEADER;
    }

    private boolean sameUser(User currentAssignee, User nextAssignee) {
        Long currentId = currentAssignee == null ? null : currentAssignee.getId();
        Long nextId = nextAssignee == null ? null : nextAssignee.getId();
        return Objects.equals(currentId, nextId);
    }

    private boolean sameColumn(BoardColumn currentColumn, BoardColumn nextColumn) {
        Long currentId = currentColumn == null ? null : currentColumn.getId();
        Long nextId = nextColumn == null ? null : nextColumn.getId();
        return Objects.equals(currentId, nextId);
    }

    private void logChange(Task task, User actor, String field, String oldValue, String newValue) {
        if (Objects.equals(oldValue, newValue)) return;
        taskChanges.changed(task, actor, "TASK_UPDATED", field, oldValue, newValue,
                actor.getFullName() + " thay đổi " + field + " từ " + String.valueOf(oldValue) + " thành " + String.valueOf(newValue), true);
    }

    private void notifyTaskAssigned(Task task, User actor) {
        if (task.getAssignedTo() == null || task.getAssignedTo().getId().equals(actor.getId())) {
            return;
        }

        notificationService.createAndSend(
                task.getAssignedTo(),
                "Cong viec moi duoc giao",
                actor.getFullName() + " da giao cong viec cho ban: " + task.getTitle(),
                NotificationType.TASK_ASSIGNED,
                task.getId()
        );
    }

    private BoardColumn resolveCompatibleColumn(Project project, Long requestedColumnId, TaskStatus requestedStatus) {
        BoardColumn column = boardService.resolveColumn(project, requestedColumnId, requestedStatus);
        if (statusForGroup(column.getStatusGroup()) != requestedStatus) {
            column = boardService.resolveColumn(project, null, requestedStatus);
        }
        if (statusForGroup(column.getStatusGroup()) != requestedStatus) {
            throw new IllegalArgumentException("Cot cong viec khong phu hop voi trang thai " + requestedStatus);
        }
        return column;
    }

    private TaskStatus statusForGroup(StatusGroup group) {
        if (group == null) {
            throw new IllegalArgumentException("Cot cong viec chua duoc gan nhom trang thai");
        }
        return switch (group) {
            case TODO -> TaskStatus.TODO;
            case IN_PROGRESS -> TaskStatus.IN_PROGRESS;
            case IN_REVIEW -> TaskStatus.REVIEW;
            case DONE -> TaskStatus.DONE;
        };
    }

    private String normalizeText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        return text.trim();
    }

    private String checklistValue(TaskChecklistItem item) {
        return item.getContent() + "|completed=" + item.isCompleted();
    }

    private String normalizeTaskType(String type) {
        if (type == null || type.isBlank()) {
            return "DESIGN";
        }

        return type.trim();
    }
}
