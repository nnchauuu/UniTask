package com.teamspace.teamspace.planning.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateWeeklyPlanRequest extends CreateWeeklyPlanRequest {
    private Long version;
}
