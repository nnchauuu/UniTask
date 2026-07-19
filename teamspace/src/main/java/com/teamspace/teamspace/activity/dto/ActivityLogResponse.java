package com.teamspace.teamspace.activity.dto;

import java.time.LocalDateTime;

import com.teamspace.teamspace.activity.entity.ActivityLog;
import com.teamspace.teamspace.activity.enums.ActivityAction;
import com.teamspace.teamspace.workspace.dto.WorkspaceUserSummary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ActivityLogResponse {

    private Long id;
    private Long projectId;
    private WorkspaceUserSummary actor;
    private ActivityAction action;
    private String targetType;
    private Long targetId;
    private String description;
    private LocalDateTime createdAt;

    public static ActivityLogResponse from(ActivityLog activityLog) {
        return ActivityLogResponse.builder()
                .id(activityLog.getId())
                .projectId(activityLog.getProject().getId())
                .actor(WorkspaceUserSummary.from(activityLog.getActor()))
                .action(activityLog.getAction())
                .targetType(activityLog.getTargetType())
                .targetId(activityLog.getTargetId())
                .description(activityLog.getDescription())
                .createdAt(activityLog.getCreatedAt())
                .build();
    }
}
