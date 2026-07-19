package com.teamspace.teamspace.evaluation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "criterion_scores", uniqueConstraints = @UniqueConstraint(columnNames = {"member_evaluation_id", "cycle_criterion_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CriterionScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_evaluation_id", nullable = false)
    private MemberEvaluation memberEvaluation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cycle_criterion_id", nullable = false)
    private EvaluationCycleCriterion criterion;

    private Double autoScore;

    private Double manualScore;

    private Double finalScore;

    @Column(nullable = false)
    private boolean insufficientData;

    @Column(length = 1500)
    private String comment;
}
