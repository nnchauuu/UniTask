package com.teamspace.teamspace.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.teamspace.teamspace.evaluation.entity.CriterionScore;
import com.teamspace.teamspace.evaluation.entity.EvaluationCriterion;
import com.teamspace.teamspace.evaluation.entity.EvaluationCycleCriterion;
import com.teamspace.teamspace.evaluation.enums.EvaluationType;
import com.teamspace.teamspace.evaluation.enums.EvaluatorType;
import com.teamspace.teamspace.evaluation.enums.MetricKey;
import com.teamspace.teamspace.evaluation.service.EvaluationService;

class EvaluationServiceTest {

    private final EvaluationService service = new EvaluationService(
            null, null, null, null, null, null, null, null, null, null, null, null, null
    );

    @Test
    void validatesTotalWeightMustBe100() {
        List<String> errors = service.validateCriteria(List.of(
                criterion("A", 30, EvaluationType.AUTO, MetricKey.TASK_COMPLETION, null),
                criterion("B", 30, EvaluationType.MANUAL, null, EvaluatorType.LEADER),
                criterion("C", 30, EvaluationType.HYBRID, MetricKey.ON_TIME_SUBMISSION, EvaluatorType.LEADER)
        ));

        assertThat(errors).anyMatch(error -> error.contains("100"));
    }

    @Test
    void validatesAutoMustHaveMetricKey() {
        List<String> errors = service.validateCriteria(List.of(
                criterion("A", 34, EvaluationType.AUTO, null, null),
                criterion("B", 33, EvaluationType.MANUAL, null, EvaluatorType.LEADER),
                criterion("C", 33, EvaluationType.MANUAL, null, EvaluatorType.PEER)
        ));

        assertThat(errors).anyMatch(error -> error.contains("metricKey"));
    }

    @Test
    void validatesDuplicateNames() {
        List<String> errors = service.validateCriteria(List.of(
                criterion("Chat luong", 34, EvaluationType.MANUAL, null, EvaluatorType.LEADER),
                criterion("chat luong", 33, EvaluationType.MANUAL, null, EvaluatorType.LEADER),
                criterion("Dung han", 33, EvaluationType.AUTO, MetricKey.ON_TIME_SUBMISSION, null)
        ));

        assertThat(errors).anyMatch(error -> error.contains("trung"));
    }

    @Test
    void validatesCriteriaCountBetweenThreeAndEight() {
        List<String> errors = service.validateCriteria(List.of(
                criterion("A", 50, EvaluationType.AUTO, MetricKey.TASK_COMPLETION, null),
                criterion("B", 50, EvaluationType.MANUAL, null, EvaluatorType.LEADER)
        ));

        assertThat(errors).anyMatch(error -> error.contains("3 den 8"));
    }

    @Test
    void validatesHybridMustHaveManualEvaluator() {
        List<String> errors = service.validateCriteria(List.of(
                criterion("A", 34, EvaluationType.HYBRID, MetricKey.TASK_COMPLETION, null),
                criterion("B", 33, EvaluationType.MANUAL, null, EvaluatorType.LEADER),
                criterion("C", 33, EvaluationType.AUTO, MetricKey.ON_TIME_SUBMISSION, null)
        ));

        assertThat(errors).anyMatch(error -> error.contains("HYBRID"));
    }

    @Test
    void validatesSortOrderMustBeUnique() {
        List<String> errors = service.validateCriteria(List.of(
                criterion("A", 34, EvaluationType.AUTO, MetricKey.TASK_COMPLETION, null),
                criterion("B", 33, EvaluationType.MANUAL, null, EvaluatorType.LEADER),
                criterion("C", 33, EvaluationType.AUTO, MetricKey.ON_TIME_SUBMISSION, null)
        ));

        assertThat(errors).anyMatch(error -> error.contains("Thu tu"));
    }

    @Test
    void normalizesManualScore() {
        assertThat(service.normalizeManualScore(4, 5)).isEqualTo(80.0);
        assertThat(service.normalizeManualScore(8, 10)).isEqualTo(80.0);
    }

    @Test
    void combinesHybridScore() {
        assertThat(service.combineScore(EvaluationType.HYBRID, 80.0, 90.0, false)).isEqualTo(86.0);
    }

    @Test
    void autoScoreWithInsufficientDataIsNull() {
        assertThat(service.combineScore(EvaluationType.AUTO, null, null, true)).isNull();
    }

    @Test
    void calculatesWeightedTotal() {
        EvaluationCycleCriterion first = cycleCriterion("A", 40);
        EvaluationCycleCriterion second = cycleCriterion("B", 60);

        CriterionScore scoreA = CriterionScore.builder().criterion(first).finalScore(80.0).build();
        CriterionScore scoreB = CriterionScore.builder().criterion(second).finalScore(90.0).build();

        assertThat(service.weightedTotal(List.of(scoreA, scoreB))).isEqualTo(86.0);
    }

    private EvaluationCriterion criterion(
            String name,
            int weight,
            EvaluationType type,
            MetricKey metricKey,
            EvaluatorType evaluatorType
    ) {
        return EvaluationCriterion.builder()
                .name(name)
                .weight(weight)
                .evaluationType(type)
                .metricKey(metricKey)
                .manualEvaluator(evaluatorType)
                .scaleMax(10)
                .sortOrder(1)
                .active(true)
                .build();
    }

    private EvaluationCycleCriterion cycleCriterion(String name, int weight) {
        return EvaluationCycleCriterion.builder()
                .name(name)
                .weight(weight)
                .evaluationType(EvaluationType.MANUAL)
                .scaleMax(10)
                .sortOrder(1)
                .build();
    }
}
