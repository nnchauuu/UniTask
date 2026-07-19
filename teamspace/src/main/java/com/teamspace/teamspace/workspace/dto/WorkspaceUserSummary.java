package com.teamspace.teamspace.workspace.dto;

import com.teamspace.teamspace.user.entity.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class WorkspaceUserSummary {

    private Long id;
    private String fullName;
    private String email;

    public static WorkspaceUserSummary from(User user) {
        return WorkspaceUserSummary.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .build();
    }
}
