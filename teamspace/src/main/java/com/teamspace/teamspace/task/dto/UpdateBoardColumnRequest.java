package com.teamspace.teamspace.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import com.teamspace.teamspace.task.enums.StatusGroup;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
public class UpdateBoardColumnRequest {
    @NotBlank
    @Size(max = 100)
    private String label;

    @Pattern(regexp = "^#[0-9a-fA-F]{6}$")
    private String color;

    @Positive
    private Integer limit;

    private boolean collapsed;

    @NotNull
    private StatusGroup statusGroup;

    private boolean defaultForGroup;

    private Long version;
}
