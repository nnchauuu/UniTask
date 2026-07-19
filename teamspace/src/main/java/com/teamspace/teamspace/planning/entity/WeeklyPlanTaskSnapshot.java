package com.teamspace.teamspace.planning.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.teamspace.teamspace.task.entity.Task;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "weekly_plan_task_snapshots", uniqueConstraints = @UniqueConstraint(name = "uk_plan_snapshot_task", columnNames = {"weekly_plan_id", "task_id"}))
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyPlanTaskSnapshot {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "weekly_plan_id", nullable = false)
    private WeeklyPlan weeklyPlan;
    @Column(nullable = false) private Long taskId;
    @Column(nullable = false, length = 40) private String taskCode;
    @Column(nullable = false, length = 200) private String title;
    private Long assigneeId;
    @Column(length = 150) private String assigneeName;
    @Column(nullable = false, length = 30) private String priority;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal estimatedEffort;
    @Column(precision = 10, scale = 2) private BigDecimal actualEffort;
    @Column(nullable = false) private boolean completed;
    @Column(nullable = false) private Long sortOrder;
    @Column(nullable = false) private LocalDateTime capturedAt;

    public static WeeklyPlanTaskSnapshot from(WeeklyPlan plan, Task task, boolean completed, long order) {
        return WeeklyPlanTaskSnapshot.builder().weeklyPlan(plan).taskId(task.getId()).taskCode("CV-" + task.getId())
                .title(task.getTitle()).assigneeId(task.getAssignedTo() == null ? null : task.getAssignedTo().getId())
                .assigneeName(task.getAssignedTo() == null ? null : task.getAssignedTo().getFullName())
                .priority(task.getPriority().name()).estimatedEffort(task.getEstimatedEffort() == null ? BigDecimal.ZERO : task.getEstimatedEffort())
                .actualEffort(task.getActualEffort())
                .completed(completed).sortOrder(order).capturedAt(LocalDateTime.now()).build();
    }
}
