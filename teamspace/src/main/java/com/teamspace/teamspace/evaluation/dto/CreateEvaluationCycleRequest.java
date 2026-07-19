package com.teamspace.teamspace.evaluation.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateEvaluationCycleRequest {
    @NotBlank
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean startImmediately;
}
