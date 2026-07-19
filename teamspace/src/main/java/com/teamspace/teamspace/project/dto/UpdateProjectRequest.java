package com.teamspace.teamspace.project.dto;

import java.time.LocalDate;

import com.teamspace.teamspace.project.enums.ProjectStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProjectRequest {

    @NotBlank(message = "Name khong duoc rong")
    @Size(max = 150, message = "Name toi da 150 ky tu")
    private String name;

    @Size(max = 1000, message = "Description toi da 1000 ky tu")
    private String description;

    @NotNull(message = "Status khong duoc rong")
    private ProjectStatus status;

    private LocalDate startDate;

    private LocalDate endDate;

    private boolean allowCustomReviewers;
}
