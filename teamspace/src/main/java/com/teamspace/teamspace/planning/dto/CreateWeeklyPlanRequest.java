package com.teamspace.teamspace.planning.dto;

import java.time.LocalDate;
import java.math.BigDecimal;

import com.teamspace.teamspace.planning.enums.EstimateUnit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateWeeklyPlanRequest {
    @NotBlank
    @Size(max = 150)
    private String name;
    @Size(max = 1000)
    private String goal;
    @Size(max = 2000)
    private String description;
    @DecimalMin(value = "0.01")
    private BigDecimal capacity;
    @NotNull
    private EstimateUnit estimateUnit = EstimateUnit.HOURS;
    @NotNull
    private LocalDate startDate;
    @NotNull
    private LocalDate endDate;
}
