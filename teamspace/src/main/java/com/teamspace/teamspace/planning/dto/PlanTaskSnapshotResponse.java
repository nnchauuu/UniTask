package com.teamspace.teamspace.planning.dto;

import java.math.BigDecimal;

import com.teamspace.teamspace.planning.entity.WeeklyPlanTaskSnapshot;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlanTaskSnapshotResponse {
    private Long taskId;
    private String taskCode;
    private String title;
    private Long assigneeId;
    private String assigneeName;
    private String priority;
    private BigDecimal estimatedEffort;
    private BigDecimal actualEffort;
    private boolean completed;
    private Long sortOrder;

    public static PlanTaskSnapshotResponse from(WeeklyPlanTaskSnapshot item) {
        return builder().taskId(item.getTaskId()).taskCode(item.getTaskCode()).title(item.getTitle())
                .assigneeId(item.getAssigneeId()).assigneeName(item.getAssigneeName()).priority(item.getPriority())
                .estimatedEffort(item.getEstimatedEffort()).actualEffort(item.getActualEffort())
                .completed(item.isCompleted()).sortOrder(item.getSortOrder()).build();
    }
}
