package com.teamspace.teamspace.evaluation.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teamspace.teamspace.evaluation.dto.ApplyTemplateRequest;
import com.teamspace.teamspace.evaluation.dto.CreateEvaluationCycleRequest;
import com.teamspace.teamspace.evaluation.dto.CriteriaValidationResponse;
import com.teamspace.teamspace.evaluation.dto.EvaluationCriterionRequest;
import com.teamspace.teamspace.evaluation.dto.EvaluationCycleResponse;
import com.teamspace.teamspace.evaluation.dto.EvaluationTemplateResponse;
import com.teamspace.teamspace.evaluation.dto.ManualScoreRequest;
import com.teamspace.teamspace.evaluation.dto.MemberEvaluationResponse;
import com.teamspace.teamspace.evaluation.dto.ProjectEvaluationConfigResponse;
import com.teamspace.teamspace.evaluation.dto.SaveTemplateRequest;
import com.teamspace.teamspace.evaluation.service.EvaluationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class EvaluationController {

    private final EvaluationService evaluationService;

    @GetMapping("/evaluation-templates")
    public List<EvaluationTemplateResponse> getTemplates(Authentication authentication) {
        return evaluationService.getTemplates(authentication);
    }

    @GetMapping("/evaluation-templates/{templateId}")
    public EvaluationTemplateResponse getTemplate(@PathVariable Long templateId, Authentication authentication) {
        return evaluationService.getTemplate(templateId, authentication);
    }

    @GetMapping("/projects/{projectId}/evaluation/config")
    public ProjectEvaluationConfigResponse getProjectConfig(@PathVariable Long projectId, Authentication authentication) {
        return evaluationService.getProjectConfig(projectId, authentication);
    }

    @PostMapping("/projects/{projectId}/evaluation/config/apply-template")
    public ProjectEvaluationConfigResponse applyTemplate(
            @PathVariable Long projectId,
            @Valid @RequestBody ApplyTemplateRequest request,
            Authentication authentication
    ) {
        return evaluationService.applyTemplate(projectId, request, authentication);
    }

    @PutMapping("/projects/{projectId}/evaluation/config/criteria")
    public ProjectEvaluationConfigResponse updateCriteria(
            @PathVariable Long projectId,
            @Valid @RequestBody List<EvaluationCriterionRequest> request,
            Authentication authentication
    ) {
        return evaluationService.updateCriteria(projectId, request, authentication);
    }

    @PostMapping("/projects/{projectId}/evaluation/config/criteria/validate")
    public CriteriaValidationResponse validateCriteria(
            @PathVariable Long projectId,
            @Valid @RequestBody List<EvaluationCriterionRequest> request,
            Authentication authentication
    ) {
        return evaluationService.validateProjectCriteria(projectId, request, authentication);
    }

    @PostMapping("/projects/{projectId}/evaluation/config/restore")
    public ProjectEvaluationConfigResponse restoreCriteria(
            @PathVariable Long projectId,
            Authentication authentication
    ) {
        return evaluationService.restoreCriteria(projectId, authentication);
    }

    @PostMapping("/projects/{projectId}/evaluation/config/save-template")
    public EvaluationTemplateResponse saveConfigAsTemplate(
            @PathVariable Long projectId,
            @Valid @RequestBody SaveTemplateRequest request,
            Authentication authentication
    ) {
        return evaluationService.saveConfigAsTemplate(projectId, request, authentication);
    }

    @PostMapping("/projects/{projectId}/evaluation/cycles")
    public EvaluationCycleResponse createCycle(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateEvaluationCycleRequest request,
            Authentication authentication
    ) {
        return evaluationService.createCycle(projectId, request, authentication);
    }

    @GetMapping("/projects/{projectId}/evaluation/cycles")
    public List<EvaluationCycleResponse> getProjectCycles(@PathVariable Long projectId, Authentication authentication) {
        return evaluationService.getProjectCycles(projectId, authentication);
    }

    @PostMapping("/evaluation/cycles/{cycleId}/start")
    public EvaluationCycleResponse startCycle(@PathVariable Long cycleId, Authentication authentication) {
        return evaluationService.startCycle(cycleId, authentication);
    }

    @PostMapping("/evaluation/cycles/{cycleId}/calculate")
    public List<MemberEvaluationResponse> calculate(@PathVariable Long cycleId, Authentication authentication) {
        return evaluationService.calculateAutomaticScores(cycleId, authentication);
    }

    @GetMapping("/evaluation/cycles/{cycleId}/results")
    public List<MemberEvaluationResponse> getCycleResults(@PathVariable Long cycleId, Authentication authentication) {
        return evaluationService.getCycleResults(cycleId, authentication);
    }

    @PostMapping("/evaluation/cycles/{cycleId}/manual-scores")
    public MemberEvaluationResponse submitManualScores(
            @PathVariable Long cycleId,
            @Valid @RequestBody ManualScoreRequest request,
            Authentication authentication
    ) {
        return evaluationService.submitManualScores(cycleId, request, authentication);
    }

    @PostMapping("/evaluation/cycles/{cycleId}/finalize")
    public List<MemberEvaluationResponse> finalizeCycle(@PathVariable Long cycleId, Authentication authentication) {
        return evaluationService.finalizeCycle(cycleId, authentication);
    }
}
