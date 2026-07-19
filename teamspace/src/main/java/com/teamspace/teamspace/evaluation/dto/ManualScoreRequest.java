package com.teamspace.teamspace.evaluation.dto;

import java.util.List;

import com.teamspace.teamspace.evaluation.enums.ManualEvaluationType;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ManualScoreRequest {
    @NotNull
    private ManualEvaluationType evaluationType;
    private Long memberUserId;
    private List<ScoreItem> scores;

    @Getter
    @Setter
    public static class ScoreItem {
        @NotNull
        private Long criterionId;
        private int score;
        private String comment;
    }
}
