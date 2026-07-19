package com.teamspace.teamspace.dashboard.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ProjectDashboardResponse {

    private long totalTasks;
    private long todoTasks;
    private long inProgressTasks;
    private long reviewTasks;
    private long doneTasks;
    private long overdueTasks;
    private double completionRate;
    private List<ProjectMemberStatsResponse> memberStats;
}
