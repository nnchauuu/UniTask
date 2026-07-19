package com.teamspace.teamspace.project.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamspace.teamspace.activity.enums.ActivityAction;
import com.teamspace.teamspace.activity.service.ActivityLogService;
import com.teamspace.teamspace.exception.BadRequestException;
import com.teamspace.teamspace.exception.ForbiddenException;
import com.teamspace.teamspace.exception.ResourceNotFoundException;
import com.teamspace.teamspace.exception.UnauthorizedException;
import com.teamspace.teamspace.project.dto.CreateProjectRequest;
import com.teamspace.teamspace.project.dto.ProjectResponse;
import com.teamspace.teamspace.project.dto.UpdateProjectRequest;
import com.teamspace.teamspace.project.entity.Project;
import com.teamspace.teamspace.project.enums.ProjectStatus;
import com.teamspace.teamspace.project.repository.ProjectRepository;
import com.teamspace.teamspace.user.entity.User;
import com.teamspace.teamspace.user.repository.UserRepository;
import com.teamspace.teamspace.workspace.entity.WorkspaceMember;
import com.teamspace.teamspace.workspace.enums.WorkspaceRole;
import com.teamspace.teamspace.workspace.repository.WorkspaceMemberRepository;
import com.teamspace.teamspace.workspace.repository.WorkspaceRepository;
import com.teamspace.teamspace.workcategory.service.WorkCategoryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;
    private final WorkCategoryService workCategoryService;

    @Transactional
    public ProjectResponse createProject(
            Long workspaceId,
            CreateProjectRequest request,
            Authentication authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        WorkspaceMember currentMember = getCurrentMemberOrThrow(workspaceId, currentUser.getId());
        requireOwnerOrLeader(currentMember);
        validateDates(request.getStartDate(), request.getEndDate());

        Project project = Project.builder()
                .workspace(currentMember.getWorkspace())
                .name(request.getName().trim())
                .description(normalizeDescription(request.getDescription()))
                .status(request.getStatus() == null ? ProjectStatus.PLANNING : request.getStatus())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .allowCustomReviewers(request.isAllowCustomReviewers())
                .createdBy(currentUser)
                .build();

        Project savedProject = projectRepository.save(project);
        workCategoryService.initializeDefaults(savedProject, currentUser);
        activityLogService.log(
                savedProject,
                currentUser,
                ActivityAction.PROJECT_CREATED,
                "PROJECT",
                savedProject.getId(),
                currentUser.getFullName() + " created project: " + savedProject.getName()
        );

        return ProjectResponse.from(savedProject, currentMember.getRole());
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getWorkspaceProjects(Long workspaceId, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        WorkspaceMember currentMember = getCurrentMemberOrThrow(workspaceId, currentUser.getId());

        return projectRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId)
                .stream()
                .map(project -> ProjectResponse.from(project, currentMember.getRole()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectDetail(Long projectId, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        Project project = getProjectOrThrow(projectId);
        WorkspaceMember currentMember = getCurrentMemberOrThrow(project.getWorkspace().getId(), currentUser.getId());

        return ProjectResponse.from(project, currentMember.getRole());
    }

    @Transactional
    public ProjectResponse updateProject(
            Long projectId,
            UpdateProjectRequest request,
            Authentication authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        Project project = getProjectOrThrow(projectId);
        WorkspaceMember currentMember = getCurrentMemberOrThrow(project.getWorkspace().getId(), currentUser.getId());
        requireOwnerOrLeader(currentMember);
        validateDates(request.getStartDate(), request.getEndDate());

        project.setName(request.getName().trim());
        project.setDescription(normalizeDescription(request.getDescription()));
        project.setStatus(request.getStatus());
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());
        project.setAllowCustomReviewers(request.isAllowCustomReviewers());

        activityLogService.log(
                project,
                currentUser,
                ActivityAction.PROJECT_UPDATED,
                "PROJECT",
                project.getId(),
                currentUser.getFullName() + " updated project: " + project.getName()
        );

        return ProjectResponse.from(project, currentMember.getRole());
    }

    @Transactional
    public void deleteProject(Long projectId, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        Project project = getProjectOrThrow(projectId);
        WorkspaceMember currentMember = getCurrentMemberOrThrow(project.getWorkspace().getId(), currentUser.getId());
        requireOwner(currentMember);

        projectRepository.delete(project);
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

    private WorkspaceMember getCurrentMemberOrThrow(Long workspaceId, Long userId) {
        if (!workspaceRepository.existsById(workspaceId)) {
            throw new ResourceNotFoundException("Khong tim thay workspace");
        }

        return workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new ForbiddenException("Ban khong co quyen truy cap workspace nay"));
    }

    private void requireOwner(WorkspaceMember member) {
        if (member.getRole() != WorkspaceRole.OWNER) {
            throw new ForbiddenException("Chi OWNER moi duoc thuc hien hanh dong nay");
        }
    }

    private void requireOwnerOrLeader(WorkspaceMember member) {
        if (member.getRole() != WorkspaceRole.OWNER && member.getRole() != WorkspaceRole.LEADER) {
            throw new ForbiddenException("Chi OWNER hoac LEADER moi duoc thuc hien hanh dong nay");
        }
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new BadRequestException("Ngay ket thuc phai sau hoac bang ngay bat dau");
        }
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }

        return description.trim();
    }
}
