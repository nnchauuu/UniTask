package com.teamspace.teamspace.planning.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReorderPlanTasksRequest {
    @NotEmpty
    private List<Long> taskIds;
}
