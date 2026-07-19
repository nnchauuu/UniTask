package com.teamspace.teamspace.evaluation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamspace.teamspace.evaluation.entity.EvaluationCycleCriterion;

public interface EvaluationCycleCriterionRepository extends JpaRepository<EvaluationCycleCriterion, Long> {
    List<EvaluationCycleCriterion> findByCycleIdOrderBySortOrderAsc(Long cycleId);
}
