package com.teamspace.teamspace.task.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.teamspace.teamspace.task.entity.TaskComment;
import com.teamspace.teamspace.workspace.dto.WorkspaceUserSummary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class TaskCommentResponse {

    private Long id;
    private Long taskId;
    private WorkspaceUserSummary author;
    private String content;
    private LocalDateTime createdAt;
    private List<WorkspaceUserSummary> mentionedUsers;

    public static TaskCommentResponse from(TaskComment comment) {
        return TaskCommentResponse.builder()
                .id(comment.getId())
                .taskId(comment.getTask().getId())
                .author(WorkspaceUserSummary.from(comment.getAuthor()))
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .mentionedUsers(comment.getMentionedUsers().stream().map(WorkspaceUserSummary::from).toList())
                .build();
    }
}
