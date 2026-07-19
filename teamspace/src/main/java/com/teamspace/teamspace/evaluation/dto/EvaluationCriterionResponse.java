package com.teamspace.teamspace.evaluation.dto;

import com.teamspace.teamspace.evaluation.entity.EvaluationCriterion;
import com.teamspace.teamspace.evaluation.entity.EvaluationCycleCriterion;
import com.teamspace.teamspace.evaluation.enums.EvaluationType;
import com.teamspace.teamspace.evaluation.enums.EvaluatorType;
import com.teamspace.teamspace.evaluation.enums.MetricKey;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EvaluationCriterionResponse {
    private Long id;
    private String name;
    private String description;
    private int weight;
    private EvaluationType evaluationType;
    private MetricKey metricKey;
    private int scaleMax;
    private EvaluatorType manualEvaluator;
    private boolean requiresEvidence;
    private boolean requiresComment;
    private int sortOrder;
    private boolean active;

    public static EvaluationCriterionResponse from(EvaluationCriterion criterion) {
        return EvaluationCriterionResponse.builder()
                .id(criterion.getId())
                .name(criterion.getName())
                .description(criterion.getDescription())
                .weight(criterion.getWeight())
                .evaluationType(criterion.getEvaluationType())
                .metricKey(criterion.getMetricKey())
                .scaleMax(criterion.getScaleMax())
                .manualEvaluator(criterion.getManualEvaluator())
                .requiresEvidence(criterion.isRequiresEvidence())
                .requiresComment(criterion.isRequiresComment())
                .sortOrder(criterion.getSortOrder())
                .active(criterion.isActive())
                .build();
    }

    public static EvaluationCriterionResponse from(EvaluationCycleCriterion criterion) {
        return EvaluationCriterionResponse.builder()
                .id(criterion.getId())
                .name(criterion.getName())
                .description(criterion.getDescription())
                .weight(criterion.getWeight())
                .evaluationType(criterion.getEvaluationType())
                .metricKey(criterion.getMetricKey())
                .scaleMax(criterion.getScaleMax())
                .manualEvaluator(criterion.getManualEvaluator())
                .requiresEvidence(criterion.isRequiresEvidence())
                .requiresComment(criterion.isRequiresComment())
                .sortOrder(criterion.getSortOrder())
                .active(true)
                .build();
    }
}
