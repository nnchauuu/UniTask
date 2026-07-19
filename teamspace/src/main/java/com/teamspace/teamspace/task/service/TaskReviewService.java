package com.teamspace.teamspace.task.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamspace.teamspace.exception.ForbiddenException;
import com.teamspace.teamspace.exception.ResourceNotFoundException;
import com.teamspace.teamspace.exception.UnauthorizedException;
import com.teamspace.teamspace.notification.enums.NotificationType;
import com.teamspace.teamspace.notification.service.NotificationService;
import com.teamspace.teamspace.task.dto.RequestChangesReviewRequest;
import com.teamspace.teamspace.task.dto.SubmitReviewRequest;
import com.teamspace.teamspace.task.dto.TaskResponse;
import com.teamspace.teamspace.task.dto.TaskReviewHistoryResponse;
import com.teamspace.teamspace.task.entity.Task;
import com.teamspace.teamspace.task.entity.TaskReviewHistory;
import com.teamspace.teamspace.task.enums.StatusGroup;
import com.teamspace.teamspace.task.enums.TaskReviewAction;
import com.teamspace.teamspace.task.enums.TaskReviewStatus;
import com.teamspace.teamspace.task.repository.TaskRepository;
import com.teamspace.teamspace.task.repository.TaskReviewHistoryRepository;
import com.teamspace.teamspace.user.entity.User;
import com.teamspace.teamspace.user.repository.UserRepository;
import com.teamspace.teamspace.workspace.entity.WorkspaceMember;
import com.teamspace.teamspace.workspace.enums.WorkspaceRole;
import com.teamspace.teamspace.workspace.repository.WorkspaceMemberRepository;
import com.teamspace.teamspace.planning.enums.PlanningState;
import com.teamspace.teamspace.realtime.service.TaskChangeCoordinator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TaskReviewService {
    private final TaskRepository taskRepository;
    private final TaskReviewHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final BoardService boardService;
    private final NotificationService notificationService;
    private final TaskChangeCoordinator taskChanges;
    private final TaskWatcherService watcherService;

    @Transactional
    public TaskResponse submit(Long taskId, SubmitReviewRequest request, Authentication authentication) {
        User actor = getCurrentUser(authentication);
        Task task = getTask(taskId);
        WorkspaceMember actorMember = getMember(task, actor);
        if (task.getPlanningState() != PlanningState.ACTIVE) throw new IllegalArgumentException("Chi task tren Board moi duoc gui duyet");
        if (task.getAssignedTo() == null || !Objects.equals(task.getAssignedTo().getId(), actor.getId())) {
            throw new ForbiddenException("Chi nguoi duoc giao task moi co the gui duyet");
        }
        if (!task.isReviewRequired()) throw new IllegalArgumentException("Task nay khong yeu cau duyet");
        if (task.getReviewStatus() != TaskReviewStatus.NONE && task.getReviewStatus() != TaskReviewStatus.CHANGES_REQUESTED) {
            throw new IllegalArgumentException("Chi task chua gui hoac can chinh sua moi duoc gui duyet");
        }
        if (task.getBoardColumn() != null && task.getBoardColumn().getStatusGroup() == StatusGroup.DONE) {
            throw new IllegalArgumentException("Task da hoan thanh khong the gui duyet");
        }

        User reviewer = resolveReviewer(task, request == null ? null : request.getReviewerId(), actor);
        task.setReviewStatus(TaskReviewStatus.PENDING);
        task.setReviewer(reviewer);
        watcherService.autoFollow(task, reviewer);
        watcherService.autoFollow(task, actor);
        task.setSubmittedBy(actor);
        task.setSubmittedAt(LocalDateTime.now());
        task.setReviewedBy(null);
        task.setReviewedAt(null);
        boardService.moveTaskToDefaultGroup(task, StatusGroup.IN_REVIEW);
        saveHistory(task, TaskReviewAction.SUBMITTED, actor, reviewer, null);
        taskChanges.event(task, "REVIEW_UPDATED");
        notify(reviewer, "Công việc chờ duyệt", actor.getFullName() + " đã gửi duyệt: " + task.getTitle(), NotificationType.TASK_REVIEW_SUBMITTED, task.getId());
        return TaskResponse.from(task, actorMember.getRole());
    }

    @Transactional
    public TaskResponse approve(Long taskId, Authentication authentication) {
        User actor = getCurrentUser(authentication);
        Task task = getTask(taskId);
        WorkspaceMember actorMember = getMember(task, actor);
        requirePending(task);
        requireReviewer(task, actor, actorMember);
        boardService.validateCanComplete(task);

        task.setReviewStatus(TaskReviewStatus.APPROVED);
        task.setReviewedBy(actor);
        task.setReviewedAt(LocalDateTime.now());
        boardService.moveTaskToDefaultGroup(task, StatusGroup.DONE);
        saveHistory(task, TaskReviewAction.APPROVED, actor, task.getReviewer(), null);
        taskChanges.event(task, "REVIEW_UPDATED");
        notify(task.getAssignedTo(), "Công việc đã được phê duyệt", actor.getFullName() + " đã phê duyệt: " + task.getTitle(), NotificationType.TASK_REVIEW_APPROVED, task.getId());
        return TaskResponse.from(task, actorMember.getRole());
    }

    @Transactional
    public TaskResponse requestChanges(Long taskId, RequestChangesReviewRequest request, Authentication authentication) {
        User actor = getCurrentUser(authentication);
        Task task = getTask(taskId);
        WorkspaceMember actorMember = getMember(task, actor);
        requirePending(task);
        requireReviewer(task, actor, actorMember);
        if (request == null || request.getReason() == null || request.getReason().isBlank()) {
            throw new IllegalArgumentException("Ly do yeu cau chinh sua khong duoc rong");
        }
        String reason = request.getReason().trim();

        task.setReviewStatus(TaskReviewStatus.CHANGES_REQUESTED);
        task.setReviewedBy(actor);
        task.setReviewedAt(LocalDateTime.now());
        boardService.moveTaskToDefaultGroup(task, StatusGroup.IN_PROGRESS);
        saveHistory(task, TaskReviewAction.CHANGES_REQUESTED, actor, task.getReviewer(), reason);
        taskChanges.event(task, "REVIEW_UPDATED");
        notify(task.getAssignedTo(), "Công việc cần chỉnh sửa", actor.getFullName() + " yêu cầu chỉnh sửa: " + task.getTitle(), NotificationType.TASK_REVIEW_CHANGES_REQUESTED, task.getId());
        return TaskResponse.from(task, actorMember.getRole());
    }

    @Transactional(readOnly = true)
    public List<TaskReviewHistoryResponse> getHistory(Long taskId, Authentication authentication) {
        Task task = getTask(taskId);
        getMember(task, getCurrentUser(authentication));
        return historyRepository.findByTaskIdOrderByCreatedAtAscIdAsc(taskId).stream()
                .map(TaskReviewHistoryResponse::from)
                .toList();
    }

    private User resolveReviewer(Task task, Long reviewerId, User submitter) {
        List<WorkspaceMember> members = memberRepository.findByWorkspaceIdOrderByJoinedAtAsc(task.getProject().getWorkspace().getId());
        if (reviewerId != null) {
            WorkspaceMember selected = members.stream().filter(member -> Objects.equals(member.getUser().getId(), reviewerId)).findFirst()
                    .orElseThrow(() -> new ForbiddenException("Nguoi duyet phai la thanh vien cua du an"));
            if (!task.getProject().isAllowCustomReviewers() && !isLeader(selected)) {
                throw new ForbiddenException("Du an chi cho phep OWNER hoac LEADER duyet task");
            }
            return selected.getUser();
        }

        Comparator<WorkspaceMember> priority = Comparator
                .comparingInt((WorkspaceMember member) -> member.getRole() == WorkspaceRole.OWNER ? 0 : 1)
                .thenComparing(WorkspaceMember::getJoinedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(member -> member.getId() == null ? Long.MAX_VALUE : member.getId());
        List<WorkspaceMember> eligible = members.stream().filter(this::isLeader).toList();
        List<WorkspaceMember> otherReviewers = eligible.stream()
                .filter(member -> !Objects.equals(member.getUser().getId(), submitter.getId()))
                .toList();
        List<WorkspaceMember> candidates = otherReviewers.isEmpty() ? eligible : otherReviewers;
        return candidates.stream().sorted(priority).map(WorkspaceMember::getUser).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Du an chua co OWNER hoac LEADER de duyet task"));
    }

    private void requirePending(Task task) {
        if (task.getReviewStatus() != TaskReviewStatus.PENDING) {
            throw new IllegalArgumentException("Chi task dang cho duyet moi duoc xu ly");
        }
    }

    private void requireReviewer(Task task, User actor, WorkspaceMember member) {
        if ((task.getReviewer() != null && Objects.equals(task.getReviewer().getId(), actor.getId())) || isLeader(member)) return;
        throw new ForbiddenException("Ban khong phai nguoi duyet cua task nay");
    }

    private boolean isLeader(WorkspaceMember member) {
        return member.getRole() == WorkspaceRole.OWNER || member.getRole() == WorkspaceRole.LEADER;
    }

    private void saveHistory(Task task, TaskReviewAction action, User actor, User reviewer, String comment) {
        historyRepository.save(TaskReviewHistory.builder().task(task).action(action).actor(actor).reviewer(reviewer).comment(comment).build());
    }

    private void notify(User receiver, String title, String content, NotificationType type, Long taskId) {
        if (receiver != null) notificationService.createAndSend(receiver, title, content, type, taskId);
    }

    private Task getTask(Long taskId) {
        return taskRepository.findById(taskId).orElseThrow(() -> new ResourceNotFoundException("Khong tim thay task"));
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) throw new UnauthorizedException("Ban chua dang nhap");
        return userRepository.findByEmail(authentication.getName()).orElseThrow(() -> new UnauthorizedException("Khong tim thay user hien tai"));
    }

    private WorkspaceMember getMember(Task task, User user) {
        return memberRepository.findByWorkspaceIdAndUserId(task.getProject().getWorkspace().getId(), user.getId())
                .orElseThrow(() -> new ForbiddenException("Ban khong thuoc du an nay"));
    }
}
