package com.teamspace.teamspace.project.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.teamspace.teamspace.project.entity.Project;
import com.teamspace.teamspace.project.enums.ProjectStatus;
import com.teamspace.teamspace.workspace.dto.WorkspaceUserSummary;
import com.teamspace.teamspace.workspace.enums.WorkspaceRole;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ProjectResponse {

    private Long id;
    private Long workspaceId;
    private String workspaceName;
    private String name;
    private String description;
    private ProjectStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private WorkspaceUserSummary createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private WorkspaceRole myRole;
    private boolean allowCustomReviewers;

    public static ProjectResponse from(Project project, WorkspaceRole myRole) {
        return ProjectResponse.builder()
                .id(project.getId())
                .workspaceId(project.getWorkspace().getId())
                .workspaceName(project.getWorkspace().getName())
                .name(project.getName())
                .description(project.getDescription())
                .status(project.getStatus())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .createdBy(WorkspaceUserSummary.from(project.getCreatedBy()))
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .myRole(myRole)
                .allowCustomReviewers(project.isAllowCustomReviewers())
                .build();
    }
}
