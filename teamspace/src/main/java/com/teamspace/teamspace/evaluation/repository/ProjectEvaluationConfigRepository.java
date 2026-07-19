package com.teamspace.teamspace.evaluation.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamspace.teamspace.evaluation.entity.ProjectEvaluationConfig;

public interface ProjectEvaluationConfigRepository extends JpaRepository<ProjectEvaluationConfig, Long> {
    Optional<ProjectEvaluationConfig> findByProjectId(Long projectId);
}
