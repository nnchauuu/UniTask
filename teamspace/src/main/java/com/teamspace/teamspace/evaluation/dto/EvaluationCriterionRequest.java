package com.teamspace.teamspace.evaluation.dto;

import com.teamspace.teamspace.evaluation.enums.EvaluationType;
import com.teamspace.teamspace.evaluation.enums.EvaluatorType;
import com.teamspace.teamspace.evaluation.enums.MetricKey;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EvaluationCriterionRequest {
    private Long id;

    @NotBlank
    private String name;

    private String description;

    @Min(1)
    private int weight;

    @NotNull
    private EvaluationType evaluationType;

    private MetricKey metricKey;

    @Min(5)
    @Max(10)
    private int scaleMax = 10;

    private EvaluatorType manualEvaluator;

    private boolean requiresEvidence;

    private boolean requiresComment;

    private int sortOrder;

    private boolean active = true;
}
