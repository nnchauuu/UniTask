package com.teamspace.teamspace.contribution.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamspace.teamspace.contribution.dto.ContributionResponse;
import com.teamspace.teamspace.exception.ForbiddenException;
import com.teamspace.teamspace.exception.ResourceNotFoundException;
import com.teamspace.teamspace.exception.UnauthorizedException;
import com.teamspace.teamspace.file.entity.FileEntity;
import com.teamspace.teamspace.file.repository.FileRepository;
import com.teamspace.teamspace.meeting.repository.MeetingParticipantRepository;
import com.teamspace.teamspace.project.entity.Project;
import com.teamspace.teamspace.project.repository.ProjectRepository;
import com.teamspace.teamspace.task.entity.Task;
import com.teamspace.teamspace.task.entity.TaskComment;
import com.teamspace.teamspace.task.enums.TaskStatus;
import com.teamspace.teamspace.task.repository.TaskCommentRepository;
import com.teamspace.teamspace.task.repository.TaskRepository;
import com.teamspace.teamspace.user.entity.User;
import com.teamspace.teamspace.user.repository.UserRepository;
import com.teamspace.teamspace.workspace.entity.WorkspaceMember;
import com.teamspace.teamspace.workspace.repository.WorkspaceMemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ContributionService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final TaskCommentRepository taskCommentRepository;
    private final FileRepository fileRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ContributionResponse> getProjectContributions(Long projectId, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay project"));

        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(project.getWorkspace().getId(), currentUser.getId())) {
            throw new ForbiddenException("Ban khong co quyen truy cap workspace nay");
        }

        List<Task> tasks = taskRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
        List<TaskComment> comments = taskCommentRepository.findByTaskProjectId(projectId);
        List<FileEntity> files = fileRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
        LocalDate today = LocalDate.now();

        return workspaceMemberRepository.findByWorkspaceIdOrderByJoinedAtAsc(project.getWorkspace().getId())
                .stream()
                .map(member -> buildContribution(member, projectId, tasks, comments, files, today))
                .sorted(Comparator.comparingInt(ContributionResponse::getScore).reversed()
                        .thenComparing(ContributionResponse::getFullName))
                .toList();
    }

    private ContributionResponse buildContribution(
            WorkspaceMember member,
            Long projectId,
            List<Task> projectTasks,
            List<TaskComment> projectComments,
            List<FileEntity> projectFiles,
            LocalDate today
    ) {
        User user = member.getUser();
        List<Task> assignedTasks = projectTasks.stream()
                .filter(task -> task.getAssignedTo() != null && task.getAssignedTo().getId().equals(user.getId()))
                .toList();

        long lateCompletedTasks = assignedTasks.stream().filter(this::isLateCompleted).count();
        long completedTasks = assignedTasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.DONE)
                .count();
        long onTimeCompletedTasks = completedTasks - lateCompletedTasks;
        long overdueTasks = assignedTasks.stream().filter(task -> isOverdue(task, today)).count();
        long commentsCount = projectComments.stream()
                .filter(comment -> comment.getAuthor().getId().equals(user.getId()))
                .count();
        long uploadedFilesCount = projectFiles.stream()
                .filter(file -> file.getUploadedBy().getId().equals(user.getId()))
                .count();
        long joinedMeetingsCount = meetingParticipantRepository.countByMeetingProjectIdAndUserId(projectId, user.getId());

        int score = (int) (
                onTimeCompletedTasks * 10
                        + lateCompletedTasks * 5
                        - overdueTasks * 5
                        + commentsCount * 2
                        + uploadedFilesCount * 3
                        + joinedMeetingsCount * 5
        );

        return ContributionResponse.builder()
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .score(score)
                .completedTasks(completedTasks)
                .lateCompletedTasks(lateCompletedTasks)
                .overdueTasks(overdueTasks)
                .commentsCount(commentsCount)
                .uploadedFilesCount(uploadedFilesCount)
                .joinedMeetingsCount(joinedMeetingsCount)
                .build();
    }

    private boolean isLateCompleted(Task task) {
        return task.getStatus() == TaskStatus.DONE
                && task.getDueDate() != null
                && task.getUpdatedAt() != null
                && task.getUpdatedAt().toLocalDate().isAfter(task.getDueDate());
    }

    private boolean isOverdue(Task task, LocalDate today) {
        return task.getStatus() != TaskStatus.DONE
                && task.getDueDate() != null
                && task.getDueDate().isBefore(today);
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new UnauthorizedException("Ban chua dang nhap");
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UnauthorizedException("Khong tim thay user hien tai"));
    }
}
