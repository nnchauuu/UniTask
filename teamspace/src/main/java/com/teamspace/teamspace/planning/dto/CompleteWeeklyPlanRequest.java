package com.teamspace.teamspace.planning.dto;

import java.time.LocalDate;

import com.teamspace.teamspace.planning.enums.IncompleteTaskAction;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompleteWeeklyPlanRequest {
    @NotNull
    private IncompleteTaskAction action;
    private Long targetPlanId;
    private String nextPlanName;
    private String nextPlanGoal;
    private LocalDate nextPlanStartDate;
    private LocalDate nextPlanEndDate;
    private Long version;
}
