package com.teamspace.teamspace.planning.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;

import com.teamspace.teamspace.planning.entity.WeeklyPlan;
import com.teamspace.teamspace.planning.enums.WeeklyPlanStatus;
import com.teamspace.teamspace.planning.enums.EstimateUnit;
import com.teamspace.teamspace.task.dto.TaskResponse;
import com.teamspace.teamspace.workspace.dto.WorkspaceUserSummary;
import com.teamspace.teamspace.workspace.enums.WorkspaceRole;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WeeklyPlanResponse {
    private Long id;
    private Long projectId;
    private String name;
    private String goal;
    private String description;
    private BigDecimal capacity;
    private EstimateUnit estimateUnit;
    private BigDecimal allocatedEffort;
    private double utilizationPercent;
    private boolean overloaded;
    private LocalDate startDate;
    private LocalDate endDate;
    private WeeklyPlanStatus status;
    private WorkspaceUserSummary createdBy;
    private WorkspaceUserSummary startedBy;
    private WorkspaceUserSummary completedBy;
    private WorkspaceUserSummary cancelledBy;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;
    private int totalTasks;
    private int completedTasks;
    private List<TaskResponse> tasks;
    private List<MemberWorkloadResponse> memberWorkloads;
    private List<PlanTaskSnapshotResponse> taskSnapshots;

    public static WeeklyPlanResponse from(WeeklyPlan plan, List<TaskResponse> tasks, List<MemberWorkloadResponse> workloads,
            List<PlanTaskSnapshotResponse> snapshots) {
        boolean useSnapshot = !snapshots.isEmpty() && plan.getStatus() == WeeklyPlanStatus.COMPLETED;
        int total = useSnapshot ? snapshots.size() : tasks.size();
        int completed = useSnapshot ? (int) snapshots.stream().filter(PlanTaskSnapshotResponse::isCompleted).count()
                : (int) tasks.stream().filter(task -> task.getStatusGroup() == com.teamspace.teamspace.task.enums.StatusGroup.DONE).count();
        BigDecimal allocated = useSnapshot
                ? snapshots.stream().map(PlanTaskSnapshotResponse::getEstimatedEffort).filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add)
                : tasks.stream().map(TaskResponse::getEstimatedEffort).filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        double utilization = plan.getCapacity() == null || plan.getCapacity().signum() <= 0 ? 0
                : allocated.multiply(BigDecimal.valueOf(100)).divide(plan.getCapacity(), 2, java.math.RoundingMode.HALF_UP).doubleValue();
        return WeeklyPlanResponse.builder()
                .id(plan.getId()).projectId(plan.getProject().getId()).name(plan.getName()).goal(plan.getGoal())
                .description(plan.getDescription()).capacity(plan.getCapacity()).estimateUnit(plan.getEstimateUnit())
                .allocatedEffort(allocated).utilizationPercent(utilization).overloaded(utilization > 100)
                .startDate(plan.getStartDate()).endDate(plan.getEndDate()).status(plan.getStatus())
                .createdBy(WorkspaceUserSummary.from(plan.getCreatedBy())).startedAt(plan.getStartedAt())
                .startedBy(plan.getStartedBy() == null ? null : WorkspaceUserSummary.from(plan.getStartedBy()))
                .completedBy(plan.getCompletedBy() == null ? null : WorkspaceUserSummary.from(plan.getCompletedBy()))
                .cancelledBy(plan.getCancelledBy() == null ? null : WorkspaceUserSummary.from(plan.getCancelledBy()))
                .completedAt(plan.getCompletedAt()).createdAt(plan.getCreatedAt()).updatedAt(plan.getUpdatedAt())
                .cancelledAt(plan.getCancelledAt()).version(plan.getVersion()).totalTasks(total).completedTasks(completed)
                .tasks(tasks).memberWorkloads(workloads).taskSnapshots(snapshots).build();
    }
}
