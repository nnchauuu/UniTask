package com.teamspace.teamspace.workspace.dto;

import com.teamspace.teamspace.workspace.enums.WorkspaceRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddWorkspaceMemberRequest {

    @NotBlank(message = "Email khong duoc rong")
    @Email(message = "Email khong dung dinh dang")
    private String email;

    @NotNull(message = "Role khong duoc null")
    private WorkspaceRole role;
}
