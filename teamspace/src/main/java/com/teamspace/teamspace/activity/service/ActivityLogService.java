package com.teamspace.teamspace.activity.service;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamspace.teamspace.activity.dto.ActivityLogResponse;
import com.teamspace.teamspace.activity.entity.ActivityLog;
import com.teamspace.teamspace.activity.enums.ActivityAction;
import com.teamspace.teamspace.activity.repository.ActivityLogRepository;
import com.teamspace.teamspace.exception.ForbiddenException;
import com.teamspace.teamspace.exception.ResourceNotFoundException;
import com.teamspace.teamspace.exception.UnauthorizedException;
import com.teamspace.teamspace.project.entity.Project;
import com.teamspace.teamspace.project.repository.ProjectRepository;
import com.teamspace.teamspace.user.entity.User;
import com.teamspace.teamspace.user.repository.UserRepository;
import com.teamspace.teamspace.workspace.repository.WorkspaceMemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    @Transactional(readOnly = true)
    public List<ActivityLogResponse> getProjectActivityLogs(Long projectId, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        Project project = getProjectOrThrow(projectId);
        requireWorkspaceMember(project, currentUser);

        return activityLogRepository.findByProjectIdOrderByCreatedAtDesc(projectId)
                .stream()
                .map(ActivityLogResponse::from)
                .toList();
    }

    @Transactional
    public void log(
            Project project,
            User actor,
            ActivityAction action,
            String targetType,
            Long targetId,
            String description
    ) {
        ActivityLog activityLog = ActivityLog.builder()
                .project(project)
                .actor(actor)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .description(description)
                .build();

        activityLogRepository.save(activityLog);
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

    private void requireWorkspaceMember(Project project, User user) {
        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(project.getWorkspace().getId(), user.getId())) {
            throw new ForbiddenException("Ban khong co quyen truy cap workspace nay");
        }
    }
}
