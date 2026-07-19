package com.teamspace.teamspace.evaluation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamspace.teamspace.evaluation.entity.EvaluationCycle;

public interface EvaluationCycleRepository extends JpaRepository<EvaluationCycle, Long> {
    List<EvaluationCycle> findByProjectIdOrderByCreatedAtDesc(Long projectId);
}
