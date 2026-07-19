package com.teamspace.teamspace.evaluation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamspace.teamspace.evaluation.entity.EvaluationCriterion;

public interface EvaluationCriterionRepository extends JpaRepository<EvaluationCriterion, Long> {
    List<EvaluationCriterion> findByProjectConfigIdOrderBySortOrderAsc(Long projectConfigId);
    List<EvaluationCriterion> findByTemplateIdOrderBySortOrderAsc(Long templateId);
}
