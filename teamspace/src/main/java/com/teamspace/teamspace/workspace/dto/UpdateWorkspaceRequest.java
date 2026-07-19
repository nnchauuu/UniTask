package com.teamspace.teamspace.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateWorkspaceRequest {

    @NotBlank(message = "Name khong duoc rong")
    @Size(max = 100, message = "Name toi da 100 ky tu")
    private String name;

    @Size(max = 500, message = "Description toi da 500 ky tu")
    private String description;
}
