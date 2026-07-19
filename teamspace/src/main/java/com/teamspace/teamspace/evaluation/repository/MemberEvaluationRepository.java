package com.teamspace.teamspace.evaluation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamspace.teamspace.evaluation.entity.MemberEvaluation;

public interface MemberEvaluationRepository extends JpaRepository<MemberEvaluation, Long> {
    List<MemberEvaluation> findByCycleId(Long cycleId);
    Optional<MemberEvaluation> findByCycleIdAndMemberId(Long cycleId, Long memberId);
}
