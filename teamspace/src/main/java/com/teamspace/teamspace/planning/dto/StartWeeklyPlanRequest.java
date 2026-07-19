package com.teamspace.teamspace.planning.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StartWeeklyPlanRequest {
    private boolean confirmOverload;
    private Long version;
}
