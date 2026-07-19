package com.teamspace.teamspace.task.dto;

import java.time.LocalDateTime;

import com.teamspace.teamspace.task.entity.TaskReviewHistory;
import com.teamspace.teamspace.task.enums.TaskReviewAction;
import com.teamspace.teamspace.workspace.dto.WorkspaceUserSummary;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TaskReviewHistoryResponse {
    private Long id;
    private TaskReviewAction action;
    private WorkspaceUserSummary actor;
    private WorkspaceUserSummary reviewer;
    private String comment;
    private LocalDateTime createdAt;

    public static TaskReviewHistoryResponse from(TaskReviewHistory history) {
        return TaskReviewHistoryResponse.builder()
                .id(history.getId())
                .action(history.getAction())
                .actor(WorkspaceUserSummary.from(history.getActor()))
                .reviewer(history.getReviewer() == null ? null : WorkspaceUserSummary.from(history.getReviewer()))
                .comment(history.getComment())
                .createdAt(history.getCreatedAt())
                .build();
    }
}
