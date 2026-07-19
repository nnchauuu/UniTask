package com.teamspace.teamspace.planning.dto;

import java.math.BigDecimal;

import com.teamspace.teamspace.workspace.dto.WorkspaceUserSummary;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberWorkloadResponse {
    private WorkspaceUserSummary member;
    private String label;
    private int taskCount;
    private BigDecimal allocatedEffort;
    private BigDecimal capacityShare;
    private double utilizationPercent;
    private boolean overloaded;
}
