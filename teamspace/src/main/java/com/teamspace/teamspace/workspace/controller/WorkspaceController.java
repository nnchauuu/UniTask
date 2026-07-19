package com.teamspace.teamspace.workspace.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teamspace.teamspace.common.ApiResponse;
import com.teamspace.teamspace.workspace.dto.AddWorkspaceMemberRequest;
import com.teamspace.teamspace.workspace.dto.CreateWorkspaceRequest;
import com.teamspace.teamspace.workspace.dto.UpdateWorkspaceMemberRoleRequest;
import com.teamspace.teamspace.workspace.dto.UpdateWorkspaceRequest;
import com.teamspace.teamspace.workspace.dto.WorkspaceDetailResponse;
import com.teamspace.teamspace.workspace.dto.WorkspaceMemberResponse;
import com.teamspace.teamspace.workspace.dto.WorkspaceResponse;
import com.teamspace.teamspace.workspace.service.WorkspaceService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @PostMapping
    public ApiResponse<WorkspaceResponse> createWorkspace(
            @Valid @RequestBody CreateWorkspaceRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(
                "Tạo không gian làm việc thành công",
                workspaceService.createWorkspace(request, authentication)
        );
    }

    @GetMapping
    public ApiResponse<List<WorkspaceResponse>> getMyWorkspaces(Authentication authentication) {
        return ApiResponse.success(
                "Lấy danh sách không gian làm việc thành công",
                workspaceService.getMyWorkspaces(authentication)
        );
    }

    @GetMapping("/{workspaceId}")
    public ApiResponse<WorkspaceDetailResponse> getWorkspaceDetail(
            @PathVariable Long workspaceId,
            Authentication authentication
    ) {
        return ApiResponse.success(
                "Lấy chi tiết không gian làm việc thành công",
                workspaceService.getWorkspaceDetail(workspaceId, authentication)
        );
    }

    @PutMapping("/{workspaceId}")
    public ApiResponse<WorkspaceResponse> updateWorkspace(
            @PathVariable Long workspaceId,
            @Valid @RequestBody UpdateWorkspaceRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(
                "Cập nhật không gian làm việc thành công",
                workspaceService.updateWorkspace(workspaceId, request, authentication)
        );
    }

    @DeleteMapping("/{workspaceId}")
    public ApiResponse<Void> deleteWorkspace(
            @PathVariable Long workspaceId,
            Authentication authentication
    ) {
        workspaceService.deleteWorkspace(workspaceId, authentication);
        return ApiResponse.success("Xóa không gian làm việc thành công");
    }

    @PostMapping("/{workspaceId}/members")
    public ApiResponse<WorkspaceMemberResponse> addMember(
            @PathVariable Long workspaceId,
            @Valid @RequestBody AddWorkspaceMemberRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(
                "Thêm thành viên vào không gian làm việc thành công",
                workspaceService.addMember(workspaceId, request, authentication)
        );
    }

    @DeleteMapping("/{workspaceId}/members/{userId}")
    public ApiResponse<Void> removeMember(
            @PathVariable Long workspaceId,
            @PathVariable Long userId,
            Authentication authentication
    ) {
        workspaceService.removeMember(workspaceId, userId, authentication);
        return ApiResponse.success("Xóa thành viên khỏi không gian làm việc thành công");
    }

    @PutMapping("/{workspaceId}/members/{userId}/role")
    public ApiResponse<WorkspaceMemberResponse> updateMemberRole(
            @PathVariable Long workspaceId,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateWorkspaceMemberRoleRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(
                "Cập nhật vai trò thành viên thành công",
                workspaceService.updateMemberRole(workspaceId, userId, request, authentication)
        );
    }
}
