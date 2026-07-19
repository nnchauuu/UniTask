package com.teamspace.teamspace.evaluation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamspace.teamspace.evaluation.entity.CriterionScore;

public interface CriterionScoreRepository extends JpaRepository<CriterionScore, Long> {
    List<CriterionScore> findByMemberEvaluationId(Long memberEvaluationId);
    Optional<CriterionScore> findByMemberEvaluationIdAndCriterionId(Long memberEvaluationId, Long criterionId);
}
