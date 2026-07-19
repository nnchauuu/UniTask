package com.teamspace.teamspace.project.dto;

import java.time.LocalDate;

import com.teamspace.teamspace.project.enums.ProjectStatus;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateProjectRequest {

    @NotBlank(message = "Name khong duoc rong")
    @Size(max = 150, message = "Name toi da 150 ky tu")
    private String name;

    @Size(max = 1000, message = "Description toi da 1000 ky tu")
    private String description;

    private ProjectStatus status = ProjectStatus.PLANNING;

    private LocalDate startDate;

    private LocalDate endDate;

    private boolean allowCustomReviewers;
}
