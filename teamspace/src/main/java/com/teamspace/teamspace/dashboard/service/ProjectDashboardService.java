package com.teamspace.teamspace.dashboard.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamspace.teamspace.dashboard.dto.ProjectDashboardResponse;
import com.teamspace.teamspace.dashboard.dto.ProjectMemberStatsResponse;
import com.teamspace.teamspace.exception.ForbiddenException;
import com.teamspace.teamspace.exception.ResourceNotFoundException;
import com.teamspace.teamspace.exception.UnauthorizedException;
import com.teamspace.teamspace.project.entity.Project;
import com.teamspace.teamspace.project.repository.ProjectRepository;
import com.teamspace.teamspace.task.entity.Task;
import com.teamspace.teamspace.task.enums.TaskStatus;
import com.teamspace.teamspace.task.repository.TaskRepository;
import com.teamspace.teamspace.user.entity.User;
import com.teamspace.teamspace.user.repository.UserRepository;
import com.teamspace.teamspace.workspace.entity.WorkspaceMember;
import com.teamspace.teamspace.workspace.repository.WorkspaceMemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProjectDashboardService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public ProjectDashboardResponse getProjectDashboard(Long projectId, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay project"));

        Long workspaceId = project.getWorkspace().getId();
        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, currentUser.getId())) {
            throw new ForbiddenException("Ban khong co quyen truy cap workspace nay");
        }

        List<Task> tasks = taskRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
        LocalDate today = LocalDate.now();

        long totalTasks = tasks.size();
        long todoTasks = countByStatus(tasks, TaskStatus.TODO);
        long inProgressTasks = countByStatus(tasks, TaskStatus.IN_PROGRESS);
        long reviewTasks = countByStatus(tasks, TaskStatus.REVIEW);
        long doneTasks = countByStatus(tasks, TaskStatus.DONE);
        long overdueTasks = tasks.stream().filter(task -> isOverdue(task, today)).count();
        double completionRate = totalTasks == 0 ? 0 : Math.round((doneTasks * 10000.0) / totalTasks) / 100.0;

        List<ProjectMemberStatsResponse> memberStats = workspaceMemberRepository
                .findByWorkspaceIdOrderByJoinedAtAsc(workspaceId)
                .stream()
                .map(member -> buildMemberStats(member, tasks, today))
                .toList();

        return ProjectDashboardResponse.builder()
                .totalTasks(totalTasks)
                .todoTasks(todoTasks)
                .inProgressTasks(inProgressTasks)
                .reviewTasks(reviewTasks)
                .doneTasks(doneTasks)
                .overdueTasks(overdueTasks)
                .completionRate(completionRate)
                .memberStats(memberStats)
                .build();
    }

    private ProjectMemberStatsResponse buildMemberStats(
            WorkspaceMember member,
            List<Task> projectTasks,
            LocalDate today
    ) {
        User user = member.getUser();
        List<Task> assignedTasks = projectTasks.stream()
                .filter(task -> task.getAssignedTo() != null && task.getAssignedTo().getId().equals(user.getId()))
                .toList();

        return ProjectMemberStatsResponse.builder()
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .totalAssignedTasks(assignedTasks.size())
                .completedTasks(countByStatus(assignedTasks, TaskStatus.DONE))
                .overdueTasks(assignedTasks.stream().filter(task -> isOverdue(task, today)).count())
                .build();
    }

    private long countByStatus(List<Task> tasks, TaskStatus status) {
        return tasks.stream().filter(task -> task.getStatus() == status).count();
    }

    private boolean isOverdue(Task task, LocalDate today) {
        return task.getDueDate() != null
                && task.getDueDate().isBefore(today)
                && task.getStatus() != TaskStatus.DONE;
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new UnauthorizedException("Ban chua dang nhap");
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UnauthorizedException("Khong tim thay user hien tai"));
    }
}
