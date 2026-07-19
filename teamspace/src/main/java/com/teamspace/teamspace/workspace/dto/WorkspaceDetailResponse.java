package com.teamspace.teamspace.workspace.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.teamspace.teamspace.workspace.entity.Workspace;
import com.teamspace.teamspace.workspace.enums.WorkspaceRole;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class WorkspaceDetailResponse {

    private Long id;
    private String name;
    private String description;
    private WorkspaceUserSummary createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private WorkspaceRole myRole;
    private List<WorkspaceMemberResponse> members;

    public static WorkspaceDetailResponse from(
            Workspace workspace,
            WorkspaceRole myRole,
            List<WorkspaceMemberResponse> members
    ) {
        return WorkspaceDetailResponse.builder()
                .id(workspace.getId())
                .name(workspace.getName())
                .description(workspace.getDescription())
                .createdBy(WorkspaceUserSummary.from(workspace.getCreatedBy()))
                .createdAt(workspace.getCreatedAt())
                .updatedAt(workspace.getUpdatedAt())
                .myRole(myRole)
                .members(members)
                .build();
    }
}
