package com.teamspace.teamspace.activity.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teamspace.teamspace.activity.dto.ActivityLogResponse;
import com.teamspace.teamspace.activity.service.ActivityLogService;
import com.teamspace.teamspace.common.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    @GetMapping("/{projectId}/activity-logs")
    public ApiResponse<List<ActivityLogResponse>> getProjectActivityLogs(
            @PathVariable Long projectId,
            Authentication authentication
    ) {
        return ApiResponse.success(
                "Activity logs fetched successfully",
                activityLogService.getProjectActivityLogs(projectId, authentication)
        );
    }
}
