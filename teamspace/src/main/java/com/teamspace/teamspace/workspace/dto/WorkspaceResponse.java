package com.teamspace.teamspace.workspace.dto;

import java.time.LocalDateTime;

import com.teamspace.teamspace.workspace.entity.Workspace;
import com.teamspace.teamspace.workspace.enums.WorkspaceRole;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class WorkspaceResponse {

    private Long id;
    private String name;
    private String description;
    private WorkspaceUserSummary createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private WorkspaceRole myRole;
    private long membersCount;

    public static WorkspaceResponse from(Workspace workspace, WorkspaceRole myRole, long membersCount) {
        return WorkspaceResponse.builder()
                .id(workspace.getId())
                .name(workspace.getName())
                .description(workspace.getDescription())
                .createdBy(WorkspaceUserSummary.from(workspace.getCreatedBy()))
                .createdAt(workspace.getCreatedAt())
                .updatedAt(workspace.getUpdatedAt())
                .myRole(myRole)
                .membersCount(membersCount)
                .build();
    }
}
