package com.teamspace.teamspace.evaluation.dto;

import com.teamspace.teamspace.evaluation.enums.TemplateLevel;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SaveTemplateRequest {
    @NotBlank
    private String name;
    private String description;
    private TemplateLevel level = TemplateLevel.CUSTOM;
}
