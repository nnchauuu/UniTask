package com.teamspace.teamspace.evaluation.dto;

import java.util.List;

import com.teamspace.teamspace.evaluation.entity.MemberEvaluation;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberEvaluationResponse {
    private Long id;
    private Long cycleId;
    private Long memberUserId;
    private String fullName;
    private String email;
    private boolean selfSubmitted;
    private boolean leaderSubmitted;
    private boolean finalized;
    private Double totalScore;
    private String finalComment;
    private List<CriterionScoreResponse> scores;

    public static MemberEvaluationResponse from(MemberEvaluation evaluation) {
        return MemberEvaluationResponse.builder()
                .id(evaluation.getId())
                .cycleId(evaluation.getCycle().getId())
                .memberUserId(evaluation.getMember().getId())
                .fullName(evaluation.getMember().getFullName())
                .email(evaluation.getMember().getEmail())
                .selfSubmitted(evaluation.isSelfSubmitted())
                .leaderSubmitted(evaluation.isLeaderSubmitted())
                .finalized(evaluation.isFinalized())
                .totalScore(evaluation.getTotalScore())
                .finalComment(evaluation.getFinalComment())
                .scores(evaluation.getScores().stream().map(CriterionScoreResponse::from).toList())
                .build();
    }
}
