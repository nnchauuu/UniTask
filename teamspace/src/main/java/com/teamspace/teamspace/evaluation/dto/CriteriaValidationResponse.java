package com.teamspace.teamspace.evaluation.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CriteriaValidationResponse {
    private int criterionCount;
    private int totalWeight;
    private boolean valid;
    private List<String> errors;
}
