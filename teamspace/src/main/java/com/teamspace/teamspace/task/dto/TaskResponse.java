package com.teamspace.teamspace.task.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

import com.teamspace.teamspace.task.entity.Task;
import com.teamspace.teamspace.task.enums.TaskPriority;
import com.teamspace.teamspace.task.enums.TaskStatus;
import com.teamspace.teamspace.task.enums.StatusGroup;
import com.teamspace.teamspace.task.enums.TaskReviewStatus;
import com.teamspace.teamspace.workspace.dto.WorkspaceUserSummary;
import com.teamspace.teamspace.workspace.enums.WorkspaceRole;
import com.teamspace.teamspace.planning.enums.PlanningState;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class TaskResponse {

    private Long id;
    private Long projectId;
    private String projectName;
    private Long workspaceId;
    private String workspaceName;
    private String title;
    private String description;
    private WorkspaceUserSummary assignedTo;
    private Long parentTaskId;
    private String parentTaskTitle;
    private int subtaskCount;
    private int completedSubtaskCount;
    private int checklistCount;
    private int completedChecklistCount;
    private Long boardColumnId;
    private String boardColumnKey;
    private StatusGroup statusGroup;
    private Long boardPosition;
    private PlanningState planningState;
    private Long weeklyPlanId;
    private String weeklyPlanName;
    private Long backlogPosition;
    private TaskStatus status;
    private TaskPriority priority;
    private String type;
    private Long workCategoryId;
    private String workCategoryName;
    private String workCategoryColor;
    private String workCategoryIcon;
    private boolean reviewRequired;
    private TaskReviewStatus reviewStatus;
    private WorkspaceUserSummary reviewer;
    private WorkspaceUserSummary submittedBy;
    private LocalDateTime submittedAt;
    private WorkspaceUserSummary reviewedBy;
    private LocalDateTime reviewedAt;
    private Long version;
    private LocalDate startDate;
    private LocalDate dueDate;
    private String labels;
    private BigDecimal estimatedEffort;
    private BigDecimal actualEffort;
    private WorkspaceUserSummary createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private WorkspaceRole myRole;

    public static TaskResponse from(Task task, WorkspaceRole myRole) {
        return TaskResponse.builder()
                .id(task.getId())
                .projectId(task.getProject().getId())
                .projectName(task.getProject().getName())
                .workspaceId(task.getProject().getWorkspace().getId())
                .workspaceName(task.getProject().getWorkspace().getName())
                .title(task.getTitle())
                .description(task.getDescription())
                .assignedTo(task.getAssignedTo() == null ? null : WorkspaceUserSummary.from(task.getAssignedTo()))
                .parentTaskId(task.getParentTask() == null ? null : task.getParentTask().getId())
                .parentTaskTitle(task.getParentTask() == null ? null : task.getParentTask().getTitle())
                .subtaskCount(task.getSubtasks().size())
                .completedSubtaskCount((int) task.getSubtasks().stream()
                        .filter(item -> item.getBoardColumn() != null && item.getBoardColumn().getStatusGroup() == StatusGroup.DONE)
                        .count())
                .checklistCount(task.getChecklistItems().size())
                .completedChecklistCount((int) task.getChecklistItems().stream().filter(item -> item.isCompleted()).count())
                .boardColumnId(task.getBoardColumn() == null ? null : task.getBoardColumn().getId())
                .boardColumnKey(task.getBoardColumn() == null ? null : task.getBoardColumn().getKey())
                .statusGroup(task.getBoardColumn() == null ? null : task.getBoardColumn().getStatusGroup())
                .boardPosition(task.getBoardPosition())
                .planningState(task.getPlanningState())
                .weeklyPlanId(task.getWeeklyPlan() == null ? null : task.getWeeklyPlan().getId())
                .weeklyPlanName(task.getWeeklyPlan() == null ? null : task.getWeeklyPlan().getName())
                .backlogPosition(task.getBacklogPosition())
                .status(task.getStatus())
                .priority(task.getPriority())
                .type(task.getType())
                .workCategoryId(task.getWorkCategory() == null ? null : task.getWorkCategory().getId())
                .workCategoryName(task.getWorkCategory() == null ? task.getType() : task.getWorkCategory().getName())
                .workCategoryColor(task.getWorkCategory() == null ? "#64748B" : task.getWorkCategory().getColor())
                .workCategoryIcon(task.getWorkCategory() == null ? "Tag" : task.getWorkCategory().getIcon())
                .reviewRequired(task.isReviewRequired())
                .reviewStatus(task.getReviewStatus())
                .reviewer(task.getReviewer() == null ? null : WorkspaceUserSummary.from(task.getReviewer()))
                .submittedBy(task.getSubmittedBy() == null ? null : WorkspaceUserSummary.from(task.getSubmittedBy()))
                .submittedAt(task.getSubmittedAt())
                .reviewedBy(task.getReviewedBy() == null ? null : WorkspaceUserSummary.from(task.getReviewedBy()))
                .reviewedAt(task.getReviewedAt())
                .version(task.getVersion())
                .startDate(task.getStartDate())
                .dueDate(task.getDueDate())
                .labels(task.getLabels())
                .estimatedEffort(task.getEstimatedEffort() == null ? BigDecimal.ZERO : task.getEstimatedEffort())
                .actualEffort(task.getActualEffort())
                .createdBy(WorkspaceUserSummary.from(task.getCreatedBy()))
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .myRole(myRole)
                .build();
    }
}
