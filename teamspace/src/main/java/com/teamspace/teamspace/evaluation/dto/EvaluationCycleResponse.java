package com.teamspace.teamspace.evaluation.dto;

import java.time.LocalDate;
import java.util.List;

import com.teamspace.teamspace.evaluation.entity.EvaluationCycle;
import com.teamspace.teamspace.evaluation.enums.EvaluationCycleStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EvaluationCycleResponse {
    private Long id;
    private Long projectId;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private EvaluationCycleStatus status;
    private boolean criteriaLocked;
    private List<EvaluationCriterionResponse> criteria;

    public static EvaluationCycleResponse from(EvaluationCycle cycle) {
        return EvaluationCycleResponse.builder()
                .id(cycle.getId())
                .projectId(cycle.getProject().getId())
                .name(cycle.getName())
                .startDate(cycle.getStartDate())
                .endDate(cycle.getEndDate())
                .status(cycle.getStatus())
                .criteriaLocked(cycle.getStatus() != EvaluationCycleStatus.DRAFT)
                .criteria(cycle.getCriteria().stream()
                        .sorted((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()))
                        .map(EvaluationCriterionResponse::from)
                        .toList())
                .build();
    }
}
