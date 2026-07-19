package com.teamspace.teamspace.evaluation.entity;

import com.teamspace.teamspace.evaluation.enums.EvaluationType;
import com.teamspace.teamspace.evaluation.enums.EvaluatorType;
import com.teamspace.teamspace.evaluation.enums.MetricKey;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "evaluation_cycle_criteria")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationCycleCriterion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cycle_id", nullable = false)
    private EvaluationCycle cycle;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private int weight;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EvaluationType evaluationType;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private MetricKey metricKey;

    @Column(nullable = false)
    private int scaleMax;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private EvaluatorType manualEvaluator;

    @Column(nullable = false)
    private boolean requiresEvidence;

    @Column(nullable = false)
    private boolean requiresComment;

    @Column(nullable = false)
    private int sortOrder;
}
