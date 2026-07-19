package com.teamspace.teamspace.task.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeleteBoardColumnRequest {
    @NotNull
    private Long destinationColumnId;
}
