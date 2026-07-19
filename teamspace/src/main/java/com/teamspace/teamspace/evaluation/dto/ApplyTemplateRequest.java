package com.teamspace.teamspace.evaluation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApplyTemplateRequest {
    @NotNull
    private Long templateId;
}
