package com.teamspace.teamspace.evaluation.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamspace.teamspace.evaluation.entity.ManualEvaluation;

public interface ManualEvaluationRepository extends JpaRepository<ManualEvaluation, Long> {
}
