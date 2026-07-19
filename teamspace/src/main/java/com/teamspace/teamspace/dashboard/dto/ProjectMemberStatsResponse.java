package com.teamspace.teamspace.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ProjectMemberStatsResponse {

    private Long userId;
    private String fullName;
    private String email;
    private long totalAssignedTasks;
    private long completedTasks;
    private long overdueTasks;
}
