package com.teamspace.teamspace.planning.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.teamspace.teamspace.common.ApiResponse;
import com.teamspace.teamspace.planning.dto.CompleteWeeklyPlanRequest;
import com.teamspace.teamspace.planning.dto.CreateWeeklyPlanRequest;
import com.teamspace.teamspace.planning.dto.MovePlanningTasksRequest;
import com.teamspace.teamspace.planning.dto.MoveSingleTaskRequest;
import com.teamspace.teamspace.planning.dto.ReorderUnplannedTasksRequest;
import com.teamspace.teamspace.planning.dto.ReorderPlanTasksRequest;
import com.teamspace.teamspace.planning.dto.StartWeeklyPlanRequest;
import com.teamspace.teamspace.planning.dto.UpdateWeeklyPlanRequest;
import com.teamspace.teamspace.planning.dto.WeeklyPlanCompletionResponse;
import com.teamspace.teamspace.planning.dto.WeeklyPlanResponse;
import com.teamspace.teamspace.planning.service.WeeklyPlanningService;
import com.teamspace.teamspace.task.dto.CreateTaskRequest;
import com.teamspace.teamspace.task.dto.TaskResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class WeeklyPlanningController {
    private final WeeklyPlanningService service;

    @GetMapping("/projects/{projectId}/unplanned-tasks")
    public ApiResponse<List<TaskResponse>> getUnplanned(@PathVariable Long projectId, Authentication authentication) {
        return ApiResponse.success("Lay danh sach cong viec chua len ke hoach thanh cong", service.getUnplanned(projectId, authentication));
    }

    @PostMapping("/projects/{projectId}/unplanned-tasks")
    public ApiResponse<TaskResponse> createUnplanned(@PathVariable Long projectId, @Valid @RequestBody CreateTaskRequest request, Authentication authentication) {
        return ApiResponse.success("Tao cong viec chua len ke hoach thanh cong", service.createUnplanned(projectId, request, authentication));
    }

    @PutMapping("/projects/{projectId}/unplanned-tasks/order")
    public ApiResponse<List<TaskResponse>> reorderUnplanned(@PathVariable Long projectId, @Valid @RequestBody ReorderUnplannedTasksRequest request, Authentication authentication) {
        return ApiResponse.success("Sap xep cong viec thanh cong", service.reorderUnplanned(projectId, request, authentication));
    }

    @PostMapping("/tasks/{taskId}/move-to-unplanned")
    public ApiResponse<List<TaskResponse>> moveToUnplanned(@PathVariable Long taskId, @RequestBody(required = false) MoveSingleTaskRequest request, Authentication authentication) {
        return ApiResponse.success("Da dua cong viec ve danh sach chua len ke hoach", service.moveToUnplanned(taskId, request != null && request.isIncludeSubtasks(), authentication));
    }

    @PostMapping("/tasks/{taskId}/move-to-board")
    public ApiResponse<List<TaskResponse>> moveToBoard(@PathVariable Long taskId, @RequestBody(required = false) MoveSingleTaskRequest request, Authentication authentication) {
        return ApiResponse.success("Da dua cong viec vao Board", service.moveToBoard(taskId, request != null && request.isIncludeSubtasks(), authentication));
    }

    @GetMapping("/projects/{projectId}/weekly-plans")
    public ApiResponse<List<WeeklyPlanResponse>> listPlans(@PathVariable Long projectId, Authentication authentication) {
        return ApiResponse.success("Lay danh sach ke hoach tuan thanh cong", service.listPlans(projectId, authentication));
    }

    @PostMapping("/projects/{projectId}/weekly-plans")
    public ApiResponse<WeeklyPlanResponse> createPlan(@PathVariable Long projectId, @Valid @RequestBody CreateWeeklyPlanRequest request, Authentication authentication) {
        return ApiResponse.success("Tao ke hoach tuan thanh cong", service.createPlan(projectId, request, authentication));
    }

    @GetMapping("/weekly-plans/{planId}")
    public ApiResponse<WeeklyPlanResponse> getPlan(@PathVariable Long planId, Authentication authentication) {
        return ApiResponse.success("Lay ke hoach tuan thanh cong", service.getPlan(planId, authentication));
    }

    @PutMapping("/weekly-plans/{planId}")
    public ApiResponse<WeeklyPlanResponse> updatePlan(@PathVariable Long planId, @Valid @RequestBody UpdateWeeklyPlanRequest request, Authentication authentication) {
        return ApiResponse.success("Cap nhat ke hoach tuan thanh cong", service.updatePlan(planId, request, authentication));
    }

    @DeleteMapping("/weekly-plans/{planId}")
    public ApiResponse<Void> deletePlan(@PathVariable Long planId, Authentication authentication) {
        service.deletePlan(planId, authentication);
        return ApiResponse.success("Xoa ke hoach tuan thanh cong", null);
    }

    @PostMapping("/weekly-plans/{planId}/tasks")
    public ApiResponse<WeeklyPlanResponse> addTasks(@PathVariable Long planId, @Valid @RequestBody MovePlanningTasksRequest request, Authentication authentication) {
        return ApiResponse.success("Them task vao ke hoach thanh cong", service.addTasks(planId, request, authentication));
    }

    @PutMapping("/weekly-plans/{planId}/tasks/order")
    public ApiResponse<WeeklyPlanResponse> reorderTasks(@PathVariable Long planId, @Valid @RequestBody ReorderPlanTasksRequest request, Authentication authentication) {
        return ApiResponse.success("Sap xep task trong ke hoach thanh cong", service.reorderPlanTasks(planId, request, authentication));
    }

    @DeleteMapping("/weekly-plans/{planId}/tasks/{taskId}")
    public ApiResponse<WeeklyPlanResponse> removeTask(@PathVariable Long planId, @PathVariable Long taskId,
            @RequestParam(defaultValue = "false") boolean includeSubtasks, Authentication authentication) {
        return ApiResponse.success("Da dua task ve danh sach chua len ke hoach", service.removeTask(planId, taskId, includeSubtasks, authentication));
    }

    @PostMapping("/weekly-plans/{planId}/start")
    public ApiResponse<WeeklyPlanResponse> startPlan(@PathVariable Long planId, @RequestBody(required = false) StartWeeklyPlanRequest request, Authentication authentication) {
        return ApiResponse.success("Bat dau ke hoach thanh cong", service.startPlan(planId, request == null ? new StartWeeklyPlanRequest() : request, authentication));
    }

    @GetMapping("/weekly-plans/{planId}/completion-preview")
    public ApiResponse<WeeklyPlanCompletionResponse> completionPreview(@PathVariable Long planId, Authentication authentication) {
        return ApiResponse.success("Tong hop ke hoach thanh cong", service.completionPreview(planId, authentication));
    }

    @PostMapping("/weekly-plans/{planId}/complete")
    public ApiResponse<WeeklyPlanCompletionResponse> completePlan(@PathVariable Long planId, @Valid @RequestBody CompleteWeeklyPlanRequest request, Authentication authentication) {
        return ApiResponse.success("Hoan thanh ke hoach thanh cong", service.completePlan(planId, request, authentication));
    }

    @PostMapping("/weekly-plans/{planId}/clone")
    public ApiResponse<WeeklyPlanResponse> clonePlan(@PathVariable Long planId, Authentication authentication) {
        return ApiResponse.success("Nhan ban ke hoach thanh cong", service.clonePlan(planId, authentication));
    }

    @PostMapping("/weekly-plans/{planId}/cancel")
    public ApiResponse<WeeklyPlanResponse> cancelPlan(@PathVariable Long planId, Authentication authentication) {
        return ApiResponse.success("Huy ke hoach thanh cong", service.cancelPlan(planId, authentication));
    }
}
