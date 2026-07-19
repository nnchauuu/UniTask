package com.teamspace.teamspace.dashboard.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teamspace.teamspace.common.ApiResponse;
import com.teamspace.teamspace.dashboard.dto.ProjectDashboardResponse;
import com.teamspace.teamspace.dashboard.service.ProjectDashboardService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectDashboardController {

    private final ProjectDashboardService projectDashboardService;

    @GetMapping("/{projectId}/dashboard")
    public ApiResponse<ProjectDashboardResponse> getProjectDashboard(
            @PathVariable Long projectId,
            Authentication authentication
    ) {
        return ApiResponse.success(
                "Lấy bảng điều khiển dự án thành công",
                projectDashboardService.getProjectDashboard(projectId, authentication)
        );
    }
}
