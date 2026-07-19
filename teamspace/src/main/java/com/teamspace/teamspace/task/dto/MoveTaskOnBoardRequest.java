package com.teamspace.teamspace.task.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MoveTaskOnBoardRequest {
    @NotNull
    private Long columnId;

    @Min(0)
    private int position;
}
