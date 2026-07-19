package com.teamspace.teamspace.task.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamspace.teamspace.exception.ForbiddenException;
import com.teamspace.teamspace.exception.ResourceNotFoundException;
import com.teamspace.teamspace.exception.UnauthorizedException;
import com.teamspace.teamspace.project.entity.Project;
import com.teamspace.teamspace.project.repository.ProjectRepository;
import com.teamspace.teamspace.task.dto.BoardColumnResponse;
import com.teamspace.teamspace.task.dto.CreateBoardColumnRequest;
import com.teamspace.teamspace.task.dto.DeleteBoardColumnRequest;
import com.teamspace.teamspace.task.dto.MoveTaskOnBoardRequest;
import com.teamspace.teamspace.task.dto.ReorderBoardColumnsRequest;
import com.teamspace.teamspace.task.dto.TaskResponse;
import com.teamspace.teamspace.task.dto.UpdateBoardColumnRequest;
import com.teamspace.teamspace.task.entity.BoardColumn;
import com.teamspace.teamspace.task.entity.Task;
import com.teamspace.teamspace.task.enums.TaskStatus;
import com.teamspace.teamspace.task.enums.StatusGroup;
import com.teamspace.teamspace.task.enums.TaskReviewStatus;
import com.teamspace.teamspace.planning.enums.PlanningState;
import com.teamspace.teamspace.realtime.service.ProjectRealtimeService;
import com.teamspace.teamspace.realtime.service.TaskChangeCoordinator;
import com.teamspace.teamspace.task.repository.BoardColumnRepository;
import com.teamspace.teamspace.task.repository.TaskRepository;
import com.teamspace.teamspace.user.entity.User;
import com.teamspace.teamspace.user.repository.UserRepository;
import com.teamspace.teamspace.workspace.entity.WorkspaceMember;
import com.teamspace.teamspace.workspace.enums.WorkspaceRole;
import com.teamspace.teamspace.workspace.repository.WorkspaceMemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BoardService {

    private static final List<DefaultColumn> DEFAULT_COLUMNS = List.of(
            new DefaultColumn("TODO", "Cần làm", "#3b82f6", StatusGroup.TODO),
            new DefaultColumn("IN_PROGRESS", "Đang thực hiện", "#7c3aed", StatusGroup.IN_PROGRESS),
            new DefaultColumn("REVIEW", "Chờ duyệt", "#a855f7", StatusGroup.IN_REVIEW),
            new DefaultColumn("DONE", "Hoàn thành", "#19a66a", StatusGroup.DONE)
    );

    private final BoardColumnRepository boardColumnRepository;
    private final TaskRepository taskRepository;
    private final ProjectRealtimeService realtimeService;
    private final TaskChangeCoordinator taskChanges;
    private final ProjectRepository projectRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;

    @Transactional
    public List<BoardColumnResponse> getColumns(Long projectId, Authentication authentication) {
        Project project = getProjectOrThrow(projectId);
        requireProjectMember(project, authentication);
        return ensureProjectBoard(project).stream().map(BoardColumnResponse::from).toList();
    }

    @Transactional
    public BoardColumnResponse createColumn(Long projectId, CreateBoardColumnRequest request, Authentication authentication) {
        Project project = getProjectOrThrow(projectId);
        requireManager(project, authentication);
        List<BoardColumn> columns = ensureProjectBoard(project);
        BoardColumn column = BoardColumn.builder()
                .project(project)
                .key("CUSTOM_" + UUID.randomUUID().toString().replace("-", ""))
                .label(request.getLabel().trim())
                .color(request.getColor() == null ? "#0f766e" : request.getColor())
                .wipLimit(request.getLimit())
                .collapsed(false)
                .position(columns.size())
                .systemColumn(false)
                .statusGroup(request.getStatusGroup())
                .build();
        column.configureGroup(request.getStatusGroup(), request.isDefaultForGroup());
        if (request.isDefaultForGroup()) clearDefaultForGroup(projectId, request.getStatusGroup(), null);
        BoardColumn saved = boardColumnRepository.save(column);
        realtimeService.publish(projectId, null, "COLUMN_CREATED", saved.getVersion(), Map.of("columnId", saved.getId()));
        return BoardColumnResponse.from(saved);
    }

    @Transactional
    public BoardColumnResponse updateColumn(Long projectId, Long columnId, UpdateBoardColumnRequest request, Authentication authentication) {
        Project project = getProjectOrThrow(projectId);
        requireManager(project, authentication);
        BoardColumn column = getProjectColumn(projectId, columnId);
        if (request.getVersion() != null && !Objects.equals(request.getVersion(), column.getVersion())) {
            throw new IllegalArgumentException("Cot da duoc cap nhat boi nguoi khac. Vui long tai lai du lieu");
        }
        column.setLabel(request.getLabel().trim());
        if (request.getColor() != null) column.setColor(request.getColor());
        column.setWipLimit(request.getLimit());
        column.setCollapsed(request.isCollapsed());
        if (request.isDefaultForGroup()) clearDefaultForGroup(projectId, request.getStatusGroup(), column.getId());
        column.configureGroup(request.getStatusGroup(), request.isDefaultForGroup());
        realtimeService.publish(projectId, null, "COLUMN_UPDATED", column.getVersion(), Map.of("columnId", column.getId()));
        return BoardColumnResponse.from(column);
    }

    @Transactional
    public List<BoardColumnResponse> reorderColumns(Long projectId, ReorderBoardColumnsRequest request, Authentication authentication) {
        Project project = getProjectOrThrow(projectId);
        requireManager(project, authentication);
        List<BoardColumn> columns = ensureProjectBoard(project);
        Map<Long, BoardColumn> byId = new HashMap<>();
        columns.forEach(column -> byId.put(column.getId(), column));
        if (request.getColumnIds().size() != columns.size()
                || new HashSet<>(request.getColumnIds()).size() != columns.size()
                || !byId.keySet().containsAll(request.getColumnIds())) {
            throw new IllegalArgumentException("Danh sach cot khong hop le");
        }
        for (int index = 0; index < request.getColumnIds().size(); index++) {
            byId.get(request.getColumnIds().get(index)).setPosition(index);
        }
        realtimeService.publish(projectId, null, "COLUMN_MOVED", null, Map.of());
        return boardColumnRepository.findByProjectIdOrderByPositionAsc(projectId)
                .stream().map(BoardColumnResponse::from).toList();
    }

    @Transactional
    public void deleteColumn(Long projectId, Long columnId, DeleteBoardColumnRequest request, Authentication authentication) {
        Project project = getProjectOrThrow(projectId);
        requireManager(project, authentication);
        List<BoardColumn> columns = ensureProjectBoard(project);
        if (columns.size() <= 1) throw new IllegalArgumentException("Board phai co it nhat mot cot");

        BoardColumn source = getProjectColumn(projectId, columnId);
        BoardColumn destination = getProjectColumn(projectId, request.getDestinationColumnId());
        if (Objects.equals(source.getId(), destination.getId())) {
            throw new IllegalArgumentException("Cot dich phai khac cot dang xoa");
        }
        if (source.isDefaultForGroup()) {
            BoardColumn replacement = columns.stream()
                    .filter(column -> !Objects.equals(column.getId(), source.getId()))
                    .filter(column -> column.getStatusGroup() == source.getStatusGroup())
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Hay tao hoac chon cot mac dinh khac cho nhom " + source.getStatusGroup() + " truoc khi xoa"));
            source.configureGroup(source.getStatusGroup(), false);
            replacement.configureGroup(replacement.getStatusGroup(), true);
        }

        List<Task> destinationTasks = new ArrayList<>(taskRepository.findByBoardColumnIdOrderByBoardPositionAscCreatedAtAsc(destination.getId()));
        List<Task> sourceTasks = taskRepository.findByBoardColumnIdOrderByBoardPositionAscCreatedAtAsc(source.getId());
        destinationTasks.addAll(sourceTasks);
        applyTaskOrder(destination, destinationTasks);
        boardColumnRepository.delete(source);
        realtimeService.publish(projectId, null, "COLUMN_DELETED", source.getVersion(), Map.of("columnId", source.getId()));

        List<BoardColumn> remaining = columns.stream().filter(column -> !Objects.equals(column.getId(), source.getId())).toList();
        for (int index = 0; index < remaining.size(); index++) remaining.get(index).setPosition(index);
    }

    @Transactional
    public TaskResponse moveTask(Long taskId, MoveTaskOnBoardRequest request, Authentication authentication) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay task"));
        Project project = task.getProject();
        if (task.getPlanningState() != PlanningState.ACTIVE) {
            throw new IllegalArgumentException("Chi task dang thuc hien moi duoc di chuyen tren Board");
        }
        User user = getCurrentUser(authentication);
        WorkspaceMember member = getProjectMember(project, user);
        if (!isManager(member) && (task.getAssignedTo() == null || !Objects.equals(task.getAssignedTo().getId(), user.getId()))) {
            throw new ForbiddenException("Ban khong co quyen di chuyen task nay");
        }

        ensureProjectBoard(project);
        BoardColumn destination = getProjectColumn(project.getId(), request.getColumnId());
        validateBoardTransition(task, destination);
        BoardColumn source = task.getBoardColumn();
        List<Task> sourceTasks = source == null
                ? new ArrayList<>()
                : new ArrayList<>(taskRepository.findByBoardColumnIdOrderByBoardPositionAscCreatedAtAsc(source.getId()));
        sourceTasks.removeIf(item -> Objects.equals(item.getId(), task.getId()));

        List<Task> destinationTasks = source != null && Objects.equals(source.getId(), destination.getId())
                ? sourceTasks
                : new ArrayList<>(taskRepository.findByBoardColumnIdOrderByBoardPositionAscCreatedAtAsc(destination.getId()));
        int targetPosition = Math.min(request.getPosition(), destinationTasks.size());
        destinationTasks.add(targetPosition, task);

        if (destination.getWipLimit() != null && destinationTasks.size() > destination.getWipLimit()) {
            throw new IllegalArgumentException("Cot da dat gioi han cong viec");
        }
        if (source != null && !Objects.equals(source.getId(), destination.getId())) applyTaskOrder(source, sourceTasks);
        applyTaskOrder(destination, destinationTasks);
        taskChanges.changed(task, user, "TASK_MOVED", "column", source == null ? null : source.getLabel(), destination.getLabel(), user.getFullName() + " chuyển công việc từ " + (source == null ? "chưa có cột" : source.getLabel()) + " sang " + destination.getLabel(), true);
        return TaskResponse.from(task, member.getRole());
    }

    @Transactional
    public List<BoardColumn> ensureProjectBoard(Project project) {
        List<BoardColumn> columns = boardColumnRepository.findByProjectIdOrderByPositionAsc(project.getId());
        if (columns.isEmpty()) {
            List<BoardColumn> defaults = new ArrayList<>();
            for (int index = 0; index < DEFAULT_COLUMNS.size(); index++) {
                DefaultColumn item = DEFAULT_COLUMNS.get(index);
                defaults.add(BoardColumn.builder()
                        .project(project)
                        .key(item.key())
                        .label(item.label())
                        .color(item.color())
                        .position(index)
                        .collapsed(false)
                        .systemColumn(true)
                        .statusGroup(item.group())
                        .defaultForGroup(true)
                        .defaultGroupKey(item.group().name())
                        .build());
            }
            columns = boardColumnRepository.saveAll(defaults);
        }

        Map<String, BoardColumn> byKey = new HashMap<>();
        columns.forEach(column -> byKey.put(column.getKey(), column));
        Map<Long, Long> nextPosition = new HashMap<>();
        columns.forEach(column -> nextPosition.put(column.getId(), (long) taskRepository.findByBoardColumnIdOrderByBoardPositionAscCreatedAtAsc(column.getId()).size()));
        for (Task task : taskRepository.findByProjectIdOrderByCreatedAtDesc(project.getId())) {
            if (task.getBoardColumn() != null) continue;
            BoardColumn column = byKey.get(task.getStatus().name());
            if (column == null) column = columns.get(0);
            task.setBoardColumn(column);
            task.setBoardPosition(nextPosition.compute(column.getId(), (id, value) -> value == null ? 1L : value + 1L) - 1L);
        }
        return columns;
    }

    public BoardColumn resolveColumn(Project project, Long columnId, TaskStatus status) {
        List<BoardColumn> columns = ensureProjectBoard(project);
        if (columnId != null) return getProjectColumn(project.getId(), columnId);
        StatusGroup group = switch (status) {
            case TODO -> StatusGroup.TODO;
            case IN_PROGRESS -> StatusGroup.IN_PROGRESS;
            case REVIEW -> StatusGroup.IN_REVIEW;
            case DONE -> StatusGroup.DONE;
        };
        return boardColumnRepository.findByProjectIdAndStatusGroupAndDefaultForGroupTrue(project.getId(), group)
                .orElseGet(() -> columns.stream().filter(column -> column.getStatusGroup() == group).findFirst().orElse(columns.get(0)));
    }

    public long nextTaskPosition(BoardColumn column) {
        return taskRepository.findByBoardColumnIdOrderByBoardPositionAscCreatedAtAsc(column.getId()).size();
    }

    private void applyTaskOrder(BoardColumn column, List<Task> tasks) {
        TaskStatus status = legacyStatus(column.getStatusGroup());
        for (int index = 0; index < tasks.size(); index++) {
            Task task = tasks.get(index);
            task.setBoardColumn(column);
            task.setBoardPosition((long) index);
            if (status != null) task.setStatus(status);
        }
    }

    public BoardColumn getDefaultColumn(Project project, StatusGroup group) {
        List<BoardColumn> columns = ensureProjectBoard(project);
        return boardColumnRepository.findByProjectIdAndStatusGroupAndDefaultForGroupTrue(project.getId(), group)
                .orElseGet(() -> columns.stream()
                        .filter(column -> column.getStatusGroup() == group)
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Du an chua co cot thuoc nhom " + group)));
    }

    public void moveTaskToDefaultGroup(Task task, StatusGroup group) {
        BoardColumn destination = getDefaultColumn(task.getProject(), group);
        List<Task> sourceTasks = task.getBoardColumn() == null
                ? new ArrayList<>()
                : new ArrayList<>(taskRepository.findByBoardColumnIdOrderByBoardPositionAscCreatedAtAsc(task.getBoardColumn().getId()));
        sourceTasks.removeIf(item -> Objects.equals(item.getId(), task.getId()));
        if (task.getBoardColumn() != null) applyTaskOrder(task.getBoardColumn(), sourceTasks);
        List<Task> destinationTasks = new ArrayList<>(taskRepository.findByBoardColumnIdOrderByBoardPositionAscCreatedAtAsc(destination.getId()));
        destinationTasks.removeIf(item -> Objects.equals(item.getId(), task.getId()));
        destinationTasks.add(task);
        applyTaskOrder(destination, destinationTasks);
    }

    public void validateCanComplete(Task task) {
        List<Task> incomplete = taskRepository.findByParentTaskIdOrderByCreatedAtAsc(task.getId()).stream()
                .filter(subtask -> subtask.getBoardColumn() == null || subtask.getBoardColumn().getStatusGroup() != StatusGroup.DONE)
                .toList();
        if (!incomplete.isEmpty()) {
            throw new IllegalArgumentException("Khong the hoan thanh task cha khi van con task con chua hoan thanh");
        }
    }

    public void validateBoardTransition(Task task, BoardColumn destination) {
        if (destination.getStatusGroup() == StatusGroup.DONE) {
            validateCanComplete(task);
            if (task.isReviewRequired() && task.getReviewStatus() != TaskReviewStatus.APPROVED) {
                throw new IllegalArgumentException("Task can duyet chi duoc chuyen sang hoan thanh sau khi phe duyet");
            }
        }
        if (destination.getStatusGroup() == StatusGroup.IN_REVIEW
                && task.isReviewRequired() && task.getReviewStatus() != TaskReviewStatus.PENDING) {
            throw new IllegalArgumentException("Hay su dung chuc nang Gui duyet de chuyen task vao nhom cho duyet");
        }
        if (task.isReviewRequired() && task.getReviewStatus() == TaskReviewStatus.PENDING
                && destination.getStatusGroup() != StatusGroup.IN_REVIEW) {
            throw new IllegalArgumentException("Task dang cho duyet chi co the duoc xu ly bang Phe duyet hoac Yeu cau chinh sua");
        }
    }

    private void clearDefaultForGroup(Long projectId, StatusGroup group, Long exceptId) {
        boardColumnRepository.findByProjectIdAndStatusGroupAndDefaultForGroupTrue(projectId, group)
                .filter(column -> exceptId == null || !Objects.equals(column.getId(), exceptId))
                .ifPresent(column -> column.configureGroup(column.getStatusGroup(), false));
    }

    private TaskStatus legacyStatus(StatusGroup group) {
        return switch (group) {
            case TODO -> TaskStatus.TODO;
            case IN_PROGRESS -> TaskStatus.IN_PROGRESS;
            case IN_REVIEW -> TaskStatus.REVIEW;
            case DONE -> TaskStatus.DONE;
        };
    }

    private TaskStatus parseSystemStatus(String key) {
        try {
            return TaskStatus.valueOf(key);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private BoardColumn getProjectColumn(Long projectId, Long columnId) {
        BoardColumn column = boardColumnRepository.findById(columnId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay cot"));
        if (!Objects.equals(column.getProject().getId(), projectId)) {
            throw new ForbiddenException("Cot khong thuoc project nay");
        }
        return column;
    }

    private Project getProjectOrThrow(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay project"));
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) throw new UnauthorizedException("Ban chua dang nhap");
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UnauthorizedException("Khong tim thay user hien tai"));
    }

    private WorkspaceMember getProjectMember(Project project, User user) {
        return workspaceMemberRepository.findByWorkspaceIdAndUserId(project.getWorkspace().getId(), user.getId())
                .orElseThrow(() -> new ForbiddenException("Ban khong co quyen truy cap project nay"));
    }

    private void requireProjectMember(Project project, Authentication authentication) {
        getProjectMember(project, getCurrentUser(authentication));
    }

    private void requireManager(Project project, Authentication authentication) {
        if (!isManager(getProjectMember(project, getCurrentUser(authentication)))) {
            throw new ForbiddenException("Chi OWNER hoac LEADER moi duoc quan ly trang thai");
        }
    }

    private boolean isManager(WorkspaceMember member) {
        return member.getRole() == WorkspaceRole.OWNER || member.getRole() == WorkspaceRole.LEADER;
    }

    private record DefaultColumn(String key, String label, String color, StatusGroup group) {}
}
