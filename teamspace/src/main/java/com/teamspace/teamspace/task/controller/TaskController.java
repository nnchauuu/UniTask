package com.teamspace.teamspace.task.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.teamspace.teamspace.common.ApiResponse;
import com.teamspace.teamspace.task.dto.CreateTaskCommentRequest;
import com.teamspace.teamspace.task.dto.CreateChecklistItemRequest;
import com.teamspace.teamspace.task.dto.ChecklistItemResponse;
import com.teamspace.teamspace.task.dto.CreateTaskRequest;
import com.teamspace.teamspace.task.dto.TaskCommentResponse;
import com.teamspace.teamspace.task.dto.TaskResponse;
import com.teamspace.teamspace.task.dto.UpdateTaskRequest;
import com.teamspace.teamspace.task.dto.UpdateTaskStatusRequest;
import com.teamspace.teamspace.task.dto.UpdateChecklistItemRequest;
import com.teamspace.teamspace.task.dto.ReorderChecklistRequest;
import com.teamspace.teamspace.task.enums.SubtaskDeleteAction;
import com.teamspace.teamspace.task.service.TaskService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping("/projects/{projectId}/tasks")
    public ApiResponse<TaskResponse> createTask(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateTaskRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(
                "Tạo công việc thành công",
                taskService.createTask(projectId, request, authentication)
        );
    }

    @GetMapping("/projects/{projectId}/tasks")
    public ApiResponse<List<TaskResponse>> getProjectTasks(
            @PathVariable Long projectId,
            Authentication authentication
    ) {
        return ApiResponse.success(
                "Lấy danh sách công việc thành công",
                taskService.getProjectTasks(projectId, authentication)
        );
    }

    @GetMapping("/tasks/{taskId}")
    public ApiResponse<TaskResponse> getTaskDetail(
            @PathVariable Long taskId,
            Authentication authentication
    ) {
        return ApiResponse.success(
                "Lấy chi tiết công việc thành công",
                taskService.getTaskDetail(taskId, authentication)
        );
    }

    @PutMapping("/tasks/{taskId}")
    public ApiResponse<TaskResponse> updateTask(
            @PathVariable Long taskId,
            @Valid @RequestBody UpdateTaskRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(
                "Cập nhật công việc thành công",
                taskService.updateTask(taskId, request, authentication)
        );
    }

    @GetMapping("/tasks/{taskId}/subtasks")
    public ApiResponse<List<TaskResponse>> getSubtasks(@PathVariable Long taskId, Authentication authentication) {
        return ApiResponse.success("Lay task con thanh cong", taskService.getSubtasks(taskId, authentication));
    }

    @DeleteMapping("/tasks/{taskId}")
    public ApiResponse<Void> deleteTask(
            @PathVariable Long taskId,
            @RequestParam(required = false) SubtaskDeleteAction subtaskAction,
            Authentication authentication
    ) {
        taskService.deleteTask(taskId, subtaskAction, authentication);
        return ApiResponse.success("Xoa task thanh cong");
    }

    @GetMapping("/tasks/{taskId}/checklist")
    public ApiResponse<List<ChecklistItemResponse>> getChecklist(@PathVariable Long taskId, Authentication authentication) {
        return ApiResponse.success("Lay checklist thanh cong", taskService.getChecklist(taskId, authentication));
    }

    @PostMapping("/tasks/{taskId}/checklist")
    public ApiResponse<ChecklistItemResponse> createChecklistItem(
            @PathVariable Long taskId,
            @Valid @RequestBody CreateChecklistItemRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success("Them checklist thanh cong", taskService.createChecklistItem(taskId, request, authentication));
    }

    @PutMapping("/tasks/{taskId}/checklist/{itemId}")
    public ApiResponse<ChecklistItemResponse> updateChecklistItem(
            @PathVariable Long taskId,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateChecklistItemRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success("Cap nhat checklist thanh cong", taskService.updateChecklistItem(taskId, itemId, request, authentication));
    }

    @DeleteMapping("/tasks/{taskId}/checklist/{itemId}")
    public ApiResponse<Void> deleteChecklistItem(
            @PathVariable Long taskId,
            @PathVariable Long itemId,
            Authentication authentication
    ) {
        taskService.deleteChecklistItem(taskId, itemId, authentication);
        return ApiResponse.success("Xoa checklist thanh cong");
    }

    @PutMapping("/tasks/{taskId}/checklist/order")
    public ApiResponse<List<ChecklistItemResponse>> reorderChecklist(
            @PathVariable Long taskId,
            @Valid @RequestBody ReorderChecklistRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success("Sap xep checklist thanh cong", taskService.reorderChecklist(taskId, request, authentication));
    }

    @PatchMapping("/tasks/{taskId}/status")
    public ApiResponse<TaskResponse> updateTaskStatus(
            @PathVariable Long taskId,
            @Valid @RequestBody UpdateTaskStatusRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(
                "Cập nhật trạng thái công việc thành công",
                taskService.updateTaskStatus(taskId, request, authentication)
        );
    }

    @GetMapping("/tasks/my")
    public ApiResponse<List<TaskResponse>> getMyTasks(Authentication authentication) {
        return ApiResponse.success(
                "Lấy công việc của tôi thành công",
                taskService.getMyTasks(authentication)
        );
    }

    @PostMapping("/tasks/{taskId}/comments")
    public ApiResponse<TaskCommentResponse> createComment(
            @PathVariable Long taskId,
            @Valid @RequestBody CreateTaskCommentRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(
                "Tạo bình luận công việc thành công",
                taskService.createComment(taskId, request, authentication)
        );
    }

    @GetMapping("/tasks/{taskId}/comments")
    public ApiResponse<List<TaskCommentResponse>> getComments(
            @PathVariable Long taskId,
            Authentication authentication
    ) {
        return ApiResponse.success(
                "Lấy bình luận công việc thành công",
                taskService.getComments(taskId, authentication)
        );
    }
}
