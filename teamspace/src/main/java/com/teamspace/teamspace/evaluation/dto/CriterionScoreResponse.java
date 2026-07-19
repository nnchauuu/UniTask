package com.teamspace.teamspace.evaluation.dto;

import com.teamspace.teamspace.evaluation.entity.CriterionScore;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CriterionScoreResponse {
    private Long criterionId;
    private String criterionName;
    private int weight;
    private Double autoScore;
    private Double manualScore;
    private Double finalScore;
    private boolean insufficientData;
    private String comment;

    public static CriterionScoreResponse from(CriterionScore score) {
        return CriterionScoreResponse.builder()
                .criterionId(score.getCriterion().getId())
                .criterionName(score.getCriterion().getName())
                .weight(score.getCriterion().getWeight())
                .autoScore(score.getAutoScore())
                .manualScore(score.getManualScore())
                .finalScore(score.getFinalScore())
                .insufficientData(score.isInsufficientData())
                .comment(score.getComment())
                .build();
    }
}
