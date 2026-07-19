package com.teamspace.teamspace.task.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teamspace.teamspace.common.ApiResponse;
import com.teamspace.teamspace.task.dto.RequestChangesReviewRequest;
import com.teamspace.teamspace.task.dto.SubmitReviewRequest;
import com.teamspace.teamspace.task.dto.TaskResponse;
import com.teamspace.teamspace.task.dto.TaskReviewHistoryResponse;
import com.teamspace.teamspace.task.service.TaskReviewService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tasks/{taskId}/reviews")
@RequiredArgsConstructor
public class TaskReviewController {
    private final TaskReviewService reviewService;

    @PostMapping("/submit")
    public ApiResponse<TaskResponse> submit(@PathVariable Long taskId, @RequestBody(required = false) SubmitReviewRequest request, Authentication authentication) {
        return ApiResponse.success("Gui duyet thanh cong", reviewService.submit(taskId, request, authentication));
    }

    @PostMapping("/approve")
    public ApiResponse<TaskResponse> approve(@PathVariable Long taskId, Authentication authentication) {
        return ApiResponse.success("Phe duyet thanh cong", reviewService.approve(taskId, authentication));
    }

    @PostMapping("/request-changes")
    public ApiResponse<TaskResponse> requestChanges(@PathVariable Long taskId, @Valid @RequestBody RequestChangesReviewRequest request, Authentication authentication) {
        return ApiResponse.success("Yeu cau chinh sua thanh cong", reviewService.requestChanges(taskId, request, authentication));
    }

    @GetMapping
    public ApiResponse<List<TaskReviewHistoryResponse>> getHistory(@PathVariable Long taskId, Authentication authentication) {
        return ApiResponse.success("Lay lich su duyet thanh cong", reviewService.getHistory(taskId, authentication));
    }
}
