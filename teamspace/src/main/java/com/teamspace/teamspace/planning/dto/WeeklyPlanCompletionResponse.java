package com.teamspace.teamspace.planning.dto;

import java.util.List;

import com.teamspace.teamspace.task.dto.TaskResponse;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WeeklyPlanCompletionResponse {
    private int totalTasks;
    private int completedTasks;
    private int incompleteTasks;
    private double completionRate;
    private java.math.BigDecimal plannedEffort;
    private java.math.BigDecimal completedEffort;
    private List<TaskResponse> incompleteTaskList;
    private WeeklyPlanResponse plan;
}
