package com.teamspace.teamspace.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.teamspace.teamspace.evaluation.entity.EvaluationCriterion;
import com.teamspace.teamspace.evaluation.entity.EvaluationTemplate;
import com.teamspace.teamspace.evaluation.enums.EvaluationType;
import com.teamspace.teamspace.evaluation.enums.EvaluatorType;
import com.teamspace.teamspace.evaluation.enums.MetricKey;
import com.teamspace.teamspace.evaluation.enums.TemplateLevel;
import com.teamspace.teamspace.evaluation.repository.EvaluationTemplateRepository;
import com.teamspace.teamspace.user.entity.Role;
import com.teamspace.teamspace.user.enums.RoleName;
import com.teamspace.teamspace.user.repository.RoleRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final EvaluationTemplateRepository evaluationTemplateRepository;

    @Override
    @Transactional
    public void run(String... args) {
        for (RoleName roleName : RoleName.values()) {
            if (!roleRepository.existsByName(roleName)) {
                roleRepository.save(Role.builder().name(roleName).build());
            }
        }
        seedEvaluationTemplates();
    }

    private void seedEvaluationTemplates() {
        upsertTemplate(TemplateLevel.BASIC, "Cơ bản", "Bộ tiêu chí gọn nhẹ cho dự án mới", template -> {
            addCriterion(template, "Tham gia hop nhom", 25, EvaluationType.AUTO, MetricKey.MEETING_ATTENDANCE, null, 1);
            addCriterion(template, "Tien do nop bai", 25, EvaluationType.AUTO, MetricKey.ON_TIME_SUBMISSION, null, 2);
            addCriterion(template, "Chat luong bai lam", 25, EvaluationType.HYBRID, MetricKey.TASK_COMPLETION, EvaluatorType.LEADER, 3);
            addCriterion(template, "Thai do hop tac", 25, EvaluationType.MANUAL, null, EvaluatorType.LEADER_AND_PEER, 4);
        });

        upsertTemplate(TemplateLevel.STANDARD, "Tiêu chuẩn", "Cân bằng giữa tiến độ, chất lượng và cộng tác", template -> {
            addCriterion(template, "Muc do hoan thanh cong viec", 25, EvaluationType.AUTO, MetricKey.TASK_COMPLETION, null, 1);
            addCriterion(template, "Chat luong ket qua", 25, EvaluationType.HYBRID, MetricKey.TASK_COMPLETION, EvaluatorType.LEADER, 2);
            addCriterion(template, "Dung thoi han", 20, EvaluationType.AUTO, MetricKey.ON_TIME_SUBMISSION, null, 3);
            addCriterion(template, "Trach nhiem va cap nhat tien do", 15, EvaluationType.HYBRID, MetricKey.PROGRESS_UPDATE, EvaluatorType.LEADER, 4);
            addCriterion(template, "Phoi hop, ho tro thanh vien", 15, EvaluationType.MANUAL, null, EvaluatorType.LEADER_AND_PEER, 5);
        });

        upsertTemplate(TemplateLevel.ADVANCED, "Nâng cao", "Đánh giá sâu về độ khó, chất lượng và tính chủ động", template -> {
            addCriterion(template, "Khoi luong va do kho cong viec", 25, EvaluationType.AUTO, MetricKey.TASK_COMPLETION, null, 1);
            addCriterion(template, "Chat luong san pham", 25, EvaluationType.HYBRID, MetricKey.TASK_COMPLETION, EvaluatorType.LEADER, 2);
            addCriterion(template, "Trach nhiem va dung cam ket", 15, EvaluationType.HYBRID, MetricKey.ON_TIME_SUBMISSION, EvaluatorType.LEADER, 3);
            addCriterion(template, "Phoi hop va ho tro nhom", 15, EvaluationType.HYBRID, MetricKey.MEETING_ATTENDANCE, EvaluatorType.LEADER_AND_PEER, 4);
            addCriterion(template, "Chu dong giai quyet van de", 10, EvaluationType.MANUAL, null, EvaluatorType.LEADER, 5);
            addCriterion(template, "Tai lieu va bao cao ket qua", 10, EvaluationType.HYBRID, MetricKey.DOCUMENT_CONTRIBUTION, EvaluatorType.LEADER, 6);
        });

        upsertTemplate(TemplateLevel.CUSTOM, "Tùy chỉnh", "Mẫu khởi tạo để nhóm tự điều chỉnh theo dự án", template -> {
            addCriterion(template, "Tiến độ công việc", 30, EvaluationType.AUTO, MetricKey.TASK_COMPLETION, null, 1);
            addCriterion(template, "Đúng thời hạn", 25, EvaluationType.AUTO, MetricKey.ON_TIME_SUBMISSION, null, 2);
            addCriterion(template, "Chất lượng kết quả", 25, EvaluationType.HYBRID, MetricKey.TASK_COMPLETION, EvaluatorType.LEADER, 3);
            addCriterion(template, "Cộng tác trong nhóm", 20, EvaluationType.MANUAL, null, EvaluatorType.LEADER_AND_PEER, 4);
        });
    }

    private void upsertTemplate(
            TemplateLevel level,
            String name,
            String description,
            java.util.function.Consumer<EvaluationTemplate> criteriaSeeder
    ) {
        EvaluationTemplate template = evaluationTemplateRepository.findByLevelAndSystemTemplate(level, true)
                .orElseGet(() -> EvaluationTemplate.builder()
                        .level(level)
                        .systemTemplate(true)
                        .build());
        template.setName(name);
        template.setDescription(description);
        if (template.getCriteria().isEmpty()) {
            criteriaSeeder.accept(template);
        }
        evaluationTemplateRepository.save(template);
    }

    private void addCriterion(
            EvaluationTemplate template,
            String name,
            int weight,
            EvaluationType type,
            MetricKey metricKey,
            EvaluatorType evaluatorType,
            int sortOrder
    ) {
        template.getCriteria().add(EvaluationCriterion.builder()
                .template(template)
                .name(name)
                .description(name)
                .weight(weight)
                .evaluationType(type)
                .metricKey(metricKey)
                .scaleMax(10)
                .manualEvaluator(evaluatorType)
                .requiresEvidence(type != EvaluationType.AUTO)
                .requiresComment(type != EvaluationType.AUTO)
                .sortOrder(sortOrder)
                .active(true)
                .build());
    }
}
