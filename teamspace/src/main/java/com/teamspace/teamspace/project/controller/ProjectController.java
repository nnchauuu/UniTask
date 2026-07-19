package com.teamspace.teamspace.project.controller;

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
import com.teamspace.teamspace.project.dto.CreateProjectRequest;
import com.teamspace.teamspace.project.dto.ProjectResponse;
import com.teamspace.teamspace.project.dto.UpdateProjectRequest;
import com.teamspace.teamspace.project.service.ProjectService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping("/workspaces/{workspaceId}/projects")
    public ApiResponse<ProjectResponse> createProject(
            @PathVariable Long workspaceId,
            @Valid @RequestBody CreateProjectRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(
                "Tạo dự án thành công",
                projectService.createProject(workspaceId, request, authentication)
        );
    }

    @GetMapping("/workspaces/{workspaceId}/projects")
    public ApiResponse<List<ProjectResponse>> getWorkspaceProjects(
            @PathVariable Long workspaceId,
            Authentication authentication
    ) {
        return ApiResponse.success(
                "Lấy danh sách dự án thành công",
                projectService.getWorkspaceProjects(workspaceId, authentication)
        );
    }

    @GetMapping("/projects/{projectId}")
    public ApiResponse<ProjectResponse> getProjectDetail(
            @PathVariable Long projectId,
            Authentication authentication
    ) {
        return ApiResponse.success(
                "Lấy chi tiết dự án thành công",
                projectService.getProjectDetail(projectId, authentication)
        );
    }

    @PutMapping("/projects/{projectId}")
    public ApiResponse<ProjectResponse> updateProject(
            @PathVariable Long projectId,
            @Valid @RequestBody UpdateProjectRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(
                "Cập nhật dự án thành công",
                projectService.updateProject(projectId, request, authentication)
        );
    }

    @DeleteMapping("/projects/{projectId}")
    public ApiResponse<Void> deleteProject(
            @PathVariable Long projectId,
            Authentication authentication
    ) {
        projectService.deleteProject(projectId, authentication);
        return ApiResponse.success("Xóa dự án thành công");
    }
}
