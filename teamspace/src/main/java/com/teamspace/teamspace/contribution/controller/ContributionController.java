package com.teamspace.teamspace.contribution.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teamspace.teamspace.common.ApiResponse;
import com.teamspace.teamspace.contribution.dto.ContributionResponse;
import com.teamspace.teamspace.contribution.service.ContributionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ContributionController {

    private final ContributionService contributionService;

    @GetMapping("/{projectId}/contributions")
    public ApiResponse<List<ContributionResponse>> getProjectContributions(
            @PathVariable Long projectId,
            Authentication authentication
    ) {
        return ApiResponse.success(
                "Contributions fetched successfully",
                contributionService.getProjectContributions(projectId, authentication)
        );
    }
}
