package com.teamspace.teamspace.evaluation.dto;

import java.util.List;

import com.teamspace.teamspace.evaluation.entity.EvaluationTemplate;
import com.teamspace.teamspace.evaluation.enums.TemplateLevel;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EvaluationTemplateResponse {
    private Long id;
    private String name;
    private String description;
    private TemplateLevel level;
    private boolean systemTemplate;
    private List<EvaluationCriterionResponse> criteria;

    public static EvaluationTemplateResponse from(EvaluationTemplate template) {
        return EvaluationTemplateResponse.builder()
                .id(template.getId())
                .name(template.getName())
                .description(template.getDescription())
                .level(template.getLevel())
                .systemTemplate(template.isSystemTemplate())
                .criteria(template.getCriteria().stream()
                        .sorted((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()))
                        .map(EvaluationCriterionResponse::from)
                        .toList())
                .build();
    }
}
