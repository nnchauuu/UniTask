package com.teamspace.teamspace.evaluation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamspace.teamspace.evaluation.entity.EvaluationTemplate;
import com.teamspace.teamspace.evaluation.enums.TemplateLevel;

public interface EvaluationTemplateRepository extends JpaRepository<EvaluationTemplate, Long> {
    boolean existsByNameAndSystemTemplate(String name, boolean systemTemplate);
    Optional<EvaluationTemplate> findByLevelAndSystemTemplate(TemplateLevel level, boolean systemTemplate);
    List<EvaluationTemplate> findBySystemTemplateTrueOrderByIdAsc();
    List<EvaluationTemplate> findBySystemTemplateTrueOrCreatedByIdOrderByIdAsc(Long createdById);
}
