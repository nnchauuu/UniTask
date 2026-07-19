package com.teamspace.teamspace.workspace.service;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamspace.teamspace.exception.BadRequestException;
import com.teamspace.teamspace.exception.ForbiddenException;
import com.teamspace.teamspace.exception.ResourceNotFoundException;
import com.teamspace.teamspace.exception.UnauthorizedException;
import com.teamspace.teamspace.user.entity.User;
import com.teamspace.teamspace.user.repository.UserRepository;
import com.teamspace.teamspace.workspace.dto.AddWorkspaceMemberRequest;
import com.teamspace.teamspace.workspace.dto.CreateWorkspaceRequest;
import com.teamspace.teamspace.workspace.dto.UpdateWorkspaceMemberRoleRequest;
import com.teamspace.teamspace.workspace.dto.UpdateWorkspaceRequest;
import com.teamspace.teamspace.workspace.dto.WorkspaceDetailResponse;
import com.teamspace.teamspace.workspace.dto.WorkspaceMemberResponse;
import com.teamspace.teamspace.workspace.dto.WorkspaceResponse;
import com.teamspace.teamspace.workspace.entity.Workspace;
import com.teamspace.teamspace.workspace.entity.WorkspaceMember;
import com.teamspace.teamspace.workspace.enums.WorkspaceRole;
import com.teamspace.teamspace.workspace.repository.WorkspaceMemberRepository;
import com.teamspace.teamspace.workspace.repository.WorkspaceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;

    @Transactional
    public WorkspaceResponse createWorkspace(CreateWorkspaceRequest request, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);

        Workspace workspace = Workspace.builder()
                .name(request.getName().trim())
                .description(normalizeDescription(request.getDescription()))
                .createdBy(currentUser)
                .build();

        Workspace savedWorkspace = workspaceRepository.save(workspace);

        WorkspaceMember ownerMember = WorkspaceMember.builder()
                .workspace(savedWorkspace)
                .user(currentUser)
                .role(WorkspaceRole.OWNER)
                .build();
        workspaceMemberRepository.save(ownerMember);

        return WorkspaceResponse.from(savedWorkspace, WorkspaceRole.OWNER, 1);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceResponse> getMyWorkspaces(Authentication authentication) {
        User currentUser = getCurrentUser(authentication);

        return workspaceMemberRepository.findByUserIdOrderByJoinedAtDesc(currentUser.getId())
                .stream()
                .map(member -> WorkspaceResponse.from(
                        member.getWorkspace(),
                        member.getRole(),
                        workspaceMemberRepository.countByWorkspaceId(member.getWorkspace().getId())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkspaceDetailResponse getWorkspaceDetail(Long workspaceId, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        WorkspaceMember currentMember = getCurrentMemberOrThrow(workspaceId, currentUser.getId());
        Workspace workspace = currentMember.getWorkspace();

        List<WorkspaceMemberResponse> members = workspaceMemberRepository
                .findByWorkspaceIdOrderByJoinedAtAsc(workspaceId)
                .stream()
                .map(WorkspaceMemberResponse::from)
                .toList();

        return WorkspaceDetailResponse.from(workspace, currentMember.getRole(), members);
    }

    @Transactional
    public WorkspaceResponse updateWorkspace(
            Long workspaceId,
            UpdateWorkspaceRequest request,
            Authentication authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        WorkspaceMember currentMember = getCurrentMemberOrThrow(workspaceId, currentUser.getId());
        requireOwnerOrLeader(currentMember);

        Workspace workspace = currentMember.getWorkspace();
        workspace.setName(request.getName().trim());
        workspace.setDescription(normalizeDescription(request.getDescription()));

        return WorkspaceResponse.from(
                workspace,
                currentMember.getRole(),
                workspaceMemberRepository.countByWorkspaceId(workspaceId)
        );
    }

    @Transactional
    public void deleteWorkspace(Long workspaceId, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        WorkspaceMember currentMember = getCurrentMemberOrThrow(workspaceId, currentUser.getId());
        requireOwner(currentMember);

        workspaceMemberRepository.deleteByWorkspaceId(workspaceId);
        workspaceRepository.delete(currentMember.getWorkspace());
    }

    @Transactional
    public WorkspaceMemberResponse addMember(
            Long workspaceId,
            AddWorkspaceMemberRequest request,
            Authentication authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        WorkspaceMember currentMember = getCurrentMemberOrThrow(workspaceId, currentUser.getId());
        requireOwnerOrLeader(currentMember);

        if (currentMember.getRole() == WorkspaceRole.LEADER && request.getRole() == WorkspaceRole.OWNER) {
            throw new ForbiddenException("Truong nhom khong duoc them thanh vien voi vai tro OWNER");
        }

        String normalizedEmail = request.getEmail().trim().toLowerCase();
        User memberUser = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay user voi email nay"));

        if (workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, memberUser.getId())) {
            throw new BadRequestException("Nguoi dung da la thanh vien cua khong gian lam viec");
        }

        WorkspaceMember newMember = WorkspaceMember.builder()
                .workspace(currentMember.getWorkspace())
                .user(memberUser)
                .role(request.getRole())
                .build();

        return WorkspaceMemberResponse.from(workspaceMemberRepository.save(newMember));
    }

    @Transactional
    public void removeMember(Long workspaceId, Long userId, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        WorkspaceMember currentMember = getCurrentMemberOrThrow(workspaceId, currentUser.getId());
        requireOwner(currentMember);

        WorkspaceMember targetMember = getCurrentMemberOrThrow(workspaceId, userId);
        ensureCanRemoveOrDowngradeOwner(workspaceId, targetMember);

        workspaceMemberRepository.delete(targetMember);
    }

    @Transactional
    public WorkspaceMemberResponse updateMemberRole(
            Long workspaceId,
            Long userId,
            UpdateWorkspaceMemberRoleRequest request,
            Authentication authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        WorkspaceMember currentMember = getCurrentMemberOrThrow(workspaceId, currentUser.getId());
        requireOwner(currentMember);

        WorkspaceMember targetMember = getCurrentMemberOrThrow(workspaceId, userId);
        if (targetMember.getRole() == WorkspaceRole.OWNER && request.getRole() != WorkspaceRole.OWNER) {
            ensureCanRemoveOrDowngradeOwner(workspaceId, targetMember);
        }

        targetMember.setRole(request.getRole());
        return WorkspaceMemberResponse.from(targetMember);
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new UnauthorizedException("Ban chua dang nhap");
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UnauthorizedException("Khong tim thay user hien tai"));
    }

    private WorkspaceMember getCurrentMemberOrThrow(Long workspaceId, Long userId) {
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

    private void ensureCanRemoveOrDowngradeOwner(Long workspaceId, WorkspaceMember targetMember) {
        if (targetMember.getRole() == WorkspaceRole.OWNER
                && workspaceMemberRepository.countByWorkspaceIdAndRole(workspaceId, WorkspaceRole.OWNER) <= 1) {
            throw new BadRequestException("Khong the xoa hoac ha quyen OWNER cuoi cung");
        }
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }

        return description.trim();
    }
}
