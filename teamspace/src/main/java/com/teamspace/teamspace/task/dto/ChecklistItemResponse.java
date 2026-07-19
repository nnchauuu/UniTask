package com.teamspace.teamspace.task.dto;

import java.time.LocalDateTime;

import com.teamspace.teamspace.task.entity.TaskChecklistItem;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChecklistItemResponse {
    private Long id;
    private String content;
    private boolean completed;
    private int position;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ChecklistItemResponse from(TaskChecklistItem item) {
        return ChecklistItemResponse.builder()
                .id(item.getId())
                .content(item.getContent())
                .completed(item.isCompleted())
                .position(item.getPosition())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
