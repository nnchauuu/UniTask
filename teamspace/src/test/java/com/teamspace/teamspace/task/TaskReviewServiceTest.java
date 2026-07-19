package com.teamspace.teamspace.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import com.teamspace.teamspace.exception.ForbiddenException;
import com.teamspace.teamspace.notification.service.NotificationService;
import com.teamspace.teamspace.project.entity.Project;
import com.teamspace.teamspace.task.dto.RequestChangesReviewRequest;
import com.teamspace.teamspace.task.dto.SubmitReviewRequest;
import com.teamspace.teamspace.task.entity.BoardColumn;
import com.teamspace.teamspace.task.entity.Task;
import com.teamspace.teamspace.task.entity.TaskReviewHistory;
import com.teamspace.teamspace.task.enums.StatusGroup;
import com.teamspace.teamspace.task.enums.TaskPriority;
import com.teamspace.teamspace.task.enums.TaskReviewStatus;
import com.teamspace.teamspace.task.enums.TaskStatus;
import com.teamspace.teamspace.task.repository.TaskRepository;
import com.teamspace.teamspace.task.repository.TaskReviewHistoryRepository;
import com.teamspace.teamspace.task.service.BoardService;
import com.teamspace.teamspace.task.service.TaskReviewService;
import com.teamspace.teamspace.task.service.TaskWatcherService;
import com.teamspace.teamspace.user.entity.User;
import com.teamspace.teamspace.user.repository.UserRepository;
import com.teamspace.teamspace.workspace.entity.Workspace;
import com.teamspace.teamspace.workspace.entity.WorkspaceMember;
import com.teamspace.teamspace.workspace.enums.WorkspaceRole;
import com.teamspace.teamspace.workspace.repository.WorkspaceMemberRepository;
import com.teamspace.teamspace.planning.enums.PlanningState;
import com.teamspace.teamspace.realtime.service.TaskChangeCoordinator;

@ExtendWith(MockitoExtension.class)
class TaskReviewServiceTest {

    @Mock TaskRepository taskRepository;
    @Mock TaskReviewHistoryRepository historyRepository;
    @Mock UserRepository userRepository;
    @Mock WorkspaceMemberRepository memberRepository;
    @Mock BoardService boardService;
    @Mock NotificationService notificationService;
    @Mock TaskChangeCoordinator taskChanges;
    @Mock TaskWatcherService watcherService;
    @Mock Authentication authentication;

    private TaskReviewService service;
    private User assignee;
    private User owner;
    private User member;
    private Workspace workspace;
    private Project project;
    private Task task;

    @BeforeEach
    void setUp() {
        service = new TaskReviewService(taskRepository, historyRepository, userRepository, memberRepository,
                boardService, notificationService, taskChanges, watcherService);
        assignee = user(1L, "assignee@example.com", "Assignee");
        owner = user(2L, "owner@example.com", "Owner");
        member = user(3L, "member@example.com", "Member");
        workspace = Workspace.builder().id(10L).name("Workspace").build();
        project = Project.builder().id(20L).name("Project").workspace(workspace).createdBy(owner).build();
        task = Task.builder()
                .id(30L).project(project).title("Review me").createdBy(owner).assignedTo(assignee)
                .status(TaskStatus.IN_PROGRESS).priority(TaskPriority.MEDIUM).type("DESIGN")
                .reviewRequired(true).reviewStatus(TaskReviewStatus.NONE)
                .planningState(PlanningState.ACTIVE)
                .boardColumn(BoardColumn.builder().id(40L).statusGroup(StatusGroup.IN_PROGRESS).key("doing").build())
                .build();
        when(taskRepository.findById(30L)).thenReturn(Optional.of(task));
    }

    @Test
    void submitsToStableOwnerAndClearsPreviousReviewMetadata() {
        authenticate(assignee, WorkspaceRole.MEMBER);
        task.setReviewStatus(TaskReviewStatus.CHANGES_REQUESTED);
        task.setReviewedBy(owner);
        task.setReviewedAt(LocalDateTime.now());
        when(memberRepository.findByWorkspaceIdOrderByJoinedAtAsc(10L)).thenReturn(List.of(
                workspaceMember(11L, member, WorkspaceRole.LEADER, 1),
                workspaceMember(12L, owner, WorkspaceRole.OWNER, 2)));

        service.submit(30L, new SubmitReviewRequest(), authentication);

        assertThat(task.getReviewStatus()).isEqualTo(TaskReviewStatus.PENDING);
        assertThat(task.getReviewer()).isEqualTo(owner);
        assertThat(task.getSubmittedBy()).isEqualTo(assignee);
        assertThat(task.getReviewedBy()).isNull();
        assertThat(task.getReviewedAt()).isNull();
        verify(boardService).moveTaskToDefaultGroup(task, StatusGroup.IN_REVIEW);
        verify(historyRepository).save(any(TaskReviewHistory.class));
    }

    @Test
    void prefersAnotherLeaderWhenSubmitterIsTheOwner() {
        task.setAssignedTo(owner);
        authenticate(owner, WorkspaceRole.OWNER);
        when(memberRepository.findByWorkspaceIdOrderByJoinedAtAsc(10L)).thenReturn(List.of(
                workspaceMember(11L, owner, WorkspaceRole.OWNER, 1),
                workspaceMember(12L, member, WorkspaceRole.LEADER, 2)));

        service.submit(30L, new SubmitReviewRequest(), authentication);

        assertThat(task.getReviewer()).isEqualTo(member);
    }

    @Test
    void rejectsDuplicateSubmissionWhilePending() {
        authenticate(assignee, WorkspaceRole.MEMBER);
        task.setReviewStatus(TaskReviewStatus.PENDING);

        assertThatThrownBy(() -> service.submit(30L, new SubmitReviewRequest(), authentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chua gui hoac can chinh sua");
        verify(historyRepository, never()).save(any());
    }

    @Test
    void rejectsApprovalByUnassignedMember() {
        task.setReviewStatus(TaskReviewStatus.PENDING);
        task.setReviewer(owner);
        authenticate(member, WorkspaceRole.MEMBER);

        assertThatThrownBy(() -> service.approve(30L, authentication))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("khong phai nguoi duyet");
    }

    @Test
    void requiresReasonWhenRequestingChanges() {
        task.setReviewStatus(TaskReviewStatus.PENDING);
        task.setReviewer(owner);
        authenticate(owner, WorkspaceRole.OWNER);
        RequestChangesReviewRequest request = new RequestChangesReviewRequest();
        request.setReason("   ");

        assertThatThrownBy(() -> service.requestChanges(30L, request, authentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("khong duoc rong");
        verify(boardService, never()).moveTaskToDefaultGroup(any(), any());
    }

    @Test
    void blocksApprovingParentWithIncompleteSubtasks() {
        task.setReviewStatus(TaskReviewStatus.PENDING);
        task.setReviewer(owner);
        authenticate(owner, WorkspaceRole.OWNER);
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Task cha van con task con chua hoan thanh"))
                .when(boardService).validateCanComplete(task);

        assertThatThrownBy(() -> service.approve(30L, authentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("task con chua hoan thanh");
        assertThat(task.getReviewStatus()).isEqualTo(TaskReviewStatus.PENDING);
        verify(boardService, never()).moveTaskToDefaultGroup(task, StatusGroup.DONE);
    }

    private void authenticate(User user, WorkspaceRole role) {
        when(authentication.getName()).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(memberRepository.findByWorkspaceIdAndUserId(10L, user.getId()))
                .thenReturn(Optional.of(workspaceMember(100L + user.getId(), user, role, 0)));
    }

    private WorkspaceMember workspaceMember(Long id, User user, WorkspaceRole role, int joinedOffset) {
        return WorkspaceMember.builder().id(id).workspace(workspace).user(user).role(role)
                .joinedAt(LocalDateTime.of(2026, 1, 1, 0, 0).plusDays(joinedOffset)).build();
    }

    private User user(Long id, String email, String name) {
        return User.builder().id(id).email(email).fullName(name).password("x").build();
    }
}
