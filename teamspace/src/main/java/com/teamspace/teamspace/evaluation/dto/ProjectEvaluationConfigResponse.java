package com.teamspace.teamspace.evaluation.dto;

import java.util.List;

import com.teamspace.teamspace.evaluation.entity.EvaluationCriterion;
import com.teamspace.teamspace.evaluation.entity.ProjectEvaluationConfig;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProjectEvaluationConfigResponse {
    private Long id;
    private Long projectId;
    private String name;
    private Long sourceTemplateId;
    private int totalWeight;
    private boolean valid;
    private List<String> validationErrors;
    private List<EvaluationCriterionResponse> criteria;

    public static ProjectEvaluationConfigResponse from(ProjectEvaluationConfig config, List<String> validationErrors) {
        return ProjectEvaluationConfigResponse.builder()
                .id(config.getId())
                .projectId(config.getProject().getId())
                .name(config.getName())
                .sourceTemplateId(config.getSourceTemplate() == null ? null : config.getSourceTemplate().getId())
                .totalWeight(config.getCriteria().stream().filter(EvaluationCriterion::isActive).mapToInt(EvaluationCriterion::getWeight).sum())
                .valid(validationErrors.isEmpty())
                .validationErrors(validationErrors)
                .criteria(config.getCriteria().stream()
                        .sorted((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()))
                        .map(EvaluationCriterionResponse::from)
                        .toList())
                .build();
    }
}
