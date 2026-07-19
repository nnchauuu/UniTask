package com.teamspace.teamspace.task.controller;

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
import com.teamspace.teamspace.task.dto.BoardColumnResponse;
import com.teamspace.teamspace.task.dto.CreateBoardColumnRequest;
import com.teamspace.teamspace.task.dto.DeleteBoardColumnRequest;
import com.teamspace.teamspace.task.dto.MoveTaskOnBoardRequest;
import com.teamspace.teamspace.task.dto.ReorderBoardColumnsRequest;
import com.teamspace.teamspace.task.dto.TaskResponse;
import com.teamspace.teamspace.task.dto.UpdateBoardColumnRequest;
import com.teamspace.teamspace.task.service.BoardService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    @GetMapping("/projects/{projectId}/board-columns")
    public ApiResponse<List<BoardColumnResponse>> getColumns(@PathVariable Long projectId, Authentication authentication) {
        return ApiResponse.success("Lay cot trang thai thanh cong", boardService.getColumns(projectId, authentication));
    }

    @PostMapping("/projects/{projectId}/board-columns")
    public ApiResponse<BoardColumnResponse> createColumn(@PathVariable Long projectId, @Valid @RequestBody CreateBoardColumnRequest request, Authentication authentication) {
        return ApiResponse.success("Tao cot thanh cong", boardService.createColumn(projectId, request, authentication));
    }

    @PutMapping("/projects/{projectId}/board-columns/{columnId}")
    public ApiResponse<BoardColumnResponse> updateColumn(@PathVariable Long projectId, @PathVariable Long columnId, @Valid @RequestBody UpdateBoardColumnRequest request, Authentication authentication) {
        return ApiResponse.success("Cap nhat cot thanh cong", boardService.updateColumn(projectId, columnId, request, authentication));
    }

    @PutMapping("/projects/{projectId}/board-columns/order")
    public ApiResponse<List<BoardColumnResponse>> reorderColumns(@PathVariable Long projectId, @Valid @RequestBody ReorderBoardColumnsRequest request, Authentication authentication) {
        return ApiResponse.success("Sap xep cot thanh cong", boardService.reorderColumns(projectId, request, authentication));
    }

    @DeleteMapping("/projects/{projectId}/board-columns/{columnId}")
    public ApiResponse<Void> deleteColumn(@PathVariable Long projectId, @PathVariable Long columnId, @Valid @RequestBody DeleteBoardColumnRequest request, Authentication authentication) {
        boardService.deleteColumn(projectId, columnId, request, authentication);
        return ApiResponse.success("Xoa cot thanh cong");
    }

    @PutMapping("/tasks/{taskId}/board-position")
    public ApiResponse<TaskResponse> moveTask(@PathVariable Long taskId, @Valid @RequestBody MoveTaskOnBoardRequest request, Authentication authentication) {
        return ApiResponse.success("Di chuyen task thanh cong", boardService.moveTask(taskId, request, authentication));
    }
}
