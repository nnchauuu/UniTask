package com.teamspace.teamspace.workspace.dto;

import com.teamspace.teamspace.workspace.enums.WorkspaceRole;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateWorkspaceMemberRoleRequest {

    @NotNull(message = "Role khong duoc null")
    private WorkspaceRole role;
}
