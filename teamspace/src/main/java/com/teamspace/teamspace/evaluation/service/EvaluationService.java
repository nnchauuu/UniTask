package com.teamspace.teamspace.evaluation.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamspace.teamspace.evaluation.dto.ApplyTemplateRequest;
import com.teamspace.teamspace.evaluation.dto.CreateEvaluationCycleRequest;
import com.teamspace.teamspace.evaluation.dto.CriteriaValidationResponse;
import com.teamspace.teamspace.evaluation.dto.EvaluationCriterionRequest;
import com.teamspace.teamspace.evaluation.dto.EvaluationCycleResponse;
import com.teamspace.teamspace.evaluation.dto.EvaluationTemplateResponse;
import com.teamspace.teamspace.evaluation.dto.ManualScoreRequest;
import com.teamspace.teamspace.evaluation.dto.MemberEvaluationResponse;
import com.teamspace.teamspace.evaluation.dto.ProjectEvaluationConfigResponse;
import com.teamspace.teamspace.evaluation.dto.SaveTemplateRequest;
import com.teamspace.teamspace.evaluation.entity.CriterionScore;
import com.teamspace.teamspace.evaluation.entity.EvaluationCriterion;
import com.teamspace.teamspace.evaluation.entity.EvaluationCycle;
import com.teamspace.teamspace.evaluation.entity.EvaluationCycleCriterion;
import com.teamspace.teamspace.evaluation.entity.EvaluationTemplate;
import com.teamspace.teamspace.evaluation.entity.ManualEvaluation;
import com.teamspace.teamspace.evaluation.entity.MemberEvaluation;
import com.teamspace.teamspace.evaluation.entity.ProjectEvaluationConfig;
import com.teamspace.teamspace.evaluation.enums.EvaluationCycleStatus;
import com.teamspace.teamspace.evaluation.enums.EvaluationType;
import com.teamspace.teamspace.evaluation.enums.ManualEvaluationType;
import com.teamspace.teamspace.evaluation.enums.MetricKey;
import com.teamspace.teamspace.evaluation.enums.TemplateLevel;
import com.teamspace.teamspace.evaluation.repository.CriterionScoreRepository;
import com.teamspace.teamspace.evaluation.repository.EvaluationCycleRepository;
import com.teamspace.teamspace.evaluation.repository.EvaluationTemplateRepository;
import com.teamspace.teamspace.evaluation.repository.ManualEvaluationRepository;
import com.teamspace.teamspace.evaluation.repository.MemberEvaluationRepository;
import com.teamspace.teamspace.evaluation.repository.ProjectEvaluationConfigRepository;
import com.teamspace.teamspace.exception.BadRequestException;
import com.teamspace.teamspace.exception.ForbiddenException;
import com.teamspace.teamspace.exception.ResourceNotFoundException;
import com.teamspace.teamspace.exception.UnauthorizedException;
import com.teamspace.teamspace.file.entity.FileEntity;
import com.teamspace.teamspace.file.repository.FileRepository;
import com.teamspace.teamspace.meeting.repository.MeetingParticipantRepository;
import com.teamspace.teamspace.meeting.repository.MeetingRepository;
import com.teamspace.teamspace.project.entity.Project;
import com.teamspace.teamspace.project.repository.ProjectRepository;
import com.teamspace.teamspace.task.entity.Task;
import com.teamspace.teamspace.task.enums.TaskPriority;
import com.teamspace.teamspace.task.enums.TaskStatus;
import com.teamspace.teamspace.task.repository.TaskRepository;
import com.teamspace.teamspace.user.entity.User;
import com.teamspace.teamspace.user.repository.UserRepository;
import com.teamspace.teamspace.workspace.entity.WorkspaceMember;
import com.teamspace.teamspace.workspace.enums.WorkspaceRole;
import com.teamspace.teamspace.workspace.repository.WorkspaceMemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final EvaluationTemplateRepository templateRepository;
    private final ProjectEvaluationConfigRepository configRepository;
    private final EvaluationCycleRepository cycleRepository;
    private final MemberEvaluationRepository memberEvaluationRepository;
    private final CriterionScoreRepository criterionScoreRepository;
    private final ManualEvaluationRepository manualEvaluationRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final FileRepository fileRepository;
    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<EvaluationTemplateResponse> getTemplates(Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        return templateRepository.findBySystemTemplateTrueOrCreatedByIdOrderByIdAsc(currentUser.getId()).stream()
                .map(EvaluationTemplateResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public EvaluationTemplateResponse getTemplate(Long templateId, Authentication authentication) {
        getCurrentUser(authentication);
        return EvaluationTemplateResponse.from(templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay mau danh gia")));
    }

    @Transactional
    public ProjectEvaluationConfigResponse getProjectConfig(Long projectId, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        Project project = getProjectAndCheckAccess(projectId, currentUser);
        lockProject(projectId);
        ProjectEvaluationConfig config = ensureConfig(project, currentUser);
        return ProjectEvaluationConfigResponse.from(config, validateCriteria(config.getCriteria()));
    }

    @Transactional
    public ProjectEvaluationConfigResponse applyTemplate(Long projectId, ApplyTemplateRequest request, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        Project project = getProjectAndCheckManager(projectId, currentUser);
        EvaluationTemplate template = templateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay mau danh gia"));
        ProjectEvaluationConfig config = ensureConfig(project, currentUser);
        config.setName(template.getName());
        config.setSourceTemplate(template);
        config.setUpdatedBy(currentUser);
        config.getCriteria().clear();
        template.getCriteria().stream()
                .sorted((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()))
                .forEach(source -> config.getCriteria().add(copyCriterionToConfig(source, config)));
        ProjectEvaluationConfig saved = configRepository.save(config);
        return ProjectEvaluationConfigResponse.from(saved, validateCriteria(saved.getCriteria()));
    }

    @Transactional
    public ProjectEvaluationConfigResponse updateCriteria(Long projectId, List<EvaluationCriterionRequest> requests, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        Project project = getProjectAndCheckManager(projectId, currentUser);
        ProjectEvaluationConfig config = ensureConfig(project, currentUser);
        List<EvaluationCriterion> replacement = requests.stream()
                .map(request -> buildCriterion(request, config))
                .toList();
        List<String> errors = validateCriteria(replacement);
        if (!errors.isEmpty()) {
            throw new BadRequestException(String.join("; ", errors));
        }
        config.getCriteria().clear();
        config.getCriteria().addAll(replacement);
        config.setUpdatedBy(currentUser);
        ProjectEvaluationConfig saved = configRepository.save(config);
        return ProjectEvaluationConfigResponse.from(saved, validateCriteria(saved.getCriteria()));
    }

    @Transactional(readOnly = true)
    public CriteriaValidationResponse validateProjectCriteria(
            Long projectId,
            List<EvaluationCriterionRequest> requests,
            Authentication authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        Project project = getProjectAndCheckAccess(projectId, currentUser);
        ProjectEvaluationConfig config = configRepository.findByProjectId(project.getId()).orElse(null);
        List<EvaluationCriterion> candidates = requests.stream()
                .map(request -> buildCriterion(request, config))
                .toList();
        List<String> errors = validateCriteria(candidates);
        return CriteriaValidationResponse.builder()
                .criterionCount((int) candidates.stream().filter(EvaluationCriterion::isActive).count())
                .totalWeight(candidates.stream().filter(EvaluationCriterion::isActive).mapToInt(EvaluationCriterion::getWeight).sum())
                .valid(errors.isEmpty())
                .errors(errors)
                .build();
    }

    @Transactional
    public ProjectEvaluationConfigResponse restoreCriteria(Long projectId, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        Project project = getProjectAndCheckManager(projectId, currentUser);
        ProjectEvaluationConfig config = ensureConfig(project, currentUser);
        EvaluationTemplate template = config.getSourceTemplate();
        if (template == null) {
            template = templateRepository.findByLevelAndSystemTemplate(TemplateLevel.BASIC, true)
                    .orElseThrow(() -> new BadRequestException("Chua co mau danh gia mac dinh"));
        }
        config.setName(template.getName());
        config.setSourceTemplate(template);
        config.setUpdatedBy(currentUser);
        config.getCriteria().clear();
        EvaluationTemplate sourceTemplate = template;
        sourceTemplate.getCriteria().stream()
                .sorted((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()))
                .forEach(source -> config.getCriteria().add(copyCriterionToConfig(source, config)));
        ProjectEvaluationConfig saved = configRepository.save(config);
        return ProjectEvaluationConfigResponse.from(saved, validateCriteria(saved.getCriteria()));
    }

    @Transactional
    public EvaluationTemplateResponse saveConfigAsTemplate(Long projectId, SaveTemplateRequest request, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        Project project = getProjectAndCheckManager(projectId, currentUser);
        ProjectEvaluationConfig config = ensureConfig(project, currentUser);
        List<String> errors = validateCriteria(config.getCriteria());
        if (!errors.isEmpty()) {
            throw new BadRequestException(String.join("; ", errors));
        }
        EvaluationTemplate template = EvaluationTemplate.builder()
                .name(request.getName())
                .description(request.getDescription())
                .level(request.getLevel())
                .systemTemplate(false)
                .createdBy(currentUser)
                .build();
        config.getCriteria().forEach(source -> template.getCriteria().add(copyCriterionToTemplate(source, template)));
        return EvaluationTemplateResponse.from(templateRepository.save(template));
    }

    @Transactional
    public EvaluationCycleResponse createCycle(Long projectId, CreateEvaluationCycleRequest request, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        Project project = getProjectAndCheckManager(projectId, currentUser);
        ProjectEvaluationConfig config = ensureConfig(project, currentUser);
        List<String> errors = validateCriteria(config.getCriteria());
        if (!errors.isEmpty()) {
            throw new BadRequestException("Cau hinh danh gia chua hop le: " + String.join("; ", errors));
        }
        if (request.getStartDate() != null && request.getEndDate() != null
                && request.getStartDate().isAfter(request.getEndDate())) {
            throw new BadRequestException("Ngay bat dau khong duoc sau ngay ket thuc");
        }
        EvaluationCycle cycle = EvaluationCycle.builder()
                .project(project)
                .name(request.getName())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(request.isStartImmediately() ? EvaluationCycleStatus.ACTIVE : EvaluationCycleStatus.DRAFT)
                .createdBy(currentUser)
                .build();
        config.getCriteria().stream()
                .filter(EvaluationCriterion::isActive)
                .sorted((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()))
                .forEach(source -> cycle.getCriteria().add(copyCriterionToCycle(source, cycle)));
        EvaluationCycle saved = cycleRepository.save(cycle);
        if (request.isStartImmediately()) {
            createMemberEvaluations(saved);
            calculateAutomaticScores(saved.getId(), currentUser);
        }
        return EvaluationCycleResponse.from(saved);
    }

    @Transactional
    public EvaluationCycleResponse startCycle(Long cycleId, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        EvaluationCycle cycle = getCycleAndCheckManager(cycleId, currentUser);
        if (cycle.getStatus() != EvaluationCycleStatus.DRAFT) {
            throw new BadRequestException("Chi co the bat dau ky dang DRAFT");
        }
        cycle.setStatus(EvaluationCycleStatus.ACTIVE);
        createMemberEvaluations(cycle);
        calculateAutomaticScores(cycleId, currentUser);
        return EvaluationCycleResponse.from(cycle);
    }

    @Transactional
    public List<MemberEvaluationResponse> calculateAutomaticScores(Long cycleId, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        return calculateAutomaticScores(cycleId, currentUser);
    }

    @Transactional
    public MemberEvaluationResponse submitManualScores(Long cycleId, ManualScoreRequest request, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        EvaluationCycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay ky danh gia"));
        ensureCycleEditable(cycle);
        User target = resolveManualTarget(request, currentUser, cycle);
        MemberEvaluation evaluation = memberEvaluationRepository.findByCycleIdAndMemberId(cycleId, target.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay danh gia thanh vien"));
        if (request.getEvaluationType() == ManualEvaluationType.LEADER) {
            getProjectAndCheckManager(cycle.getProject().getId(), currentUser);
        } else if (request.getEvaluationType() == ManualEvaluationType.SELF && !target.getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Chi duoc tu danh gia chinh minh");
        } else if (request.getEvaluationType() == ManualEvaluationType.PEER
                && !workspaceMemberRepository.existsByWorkspaceIdAndUserId(cycle.getProject().getWorkspace().getId(), currentUser.getId())) {
            throw new ForbiddenException("Ban khong co quyen danh gia dong doi");
        }
        for (ManualScoreRequest.ScoreItem item : request.getScores()) {
            CriterionScore score = criterionScoreRepository.findByMemberEvaluationIdAndCriterionId(evaluation.getId(), item.getCriterionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay tieu chi"));
            if (item.getScore() < 1 || item.getScore() > score.getCriterion().getScaleMax()) {
                throw new BadRequestException("Diem thu cong phai nam trong thang diem cau hinh");
            }
            double normalized = normalizeManualScore(item.getScore(), score.getCriterion().getScaleMax());
            score.setManualScore(normalized);
            score.setComment(item.getComment());
            score.setFinalScore(combineScore(score.getCriterion().getEvaluationType(), score.getAutoScore(), normalized, score.isInsufficientData()));
            manualEvaluationRepository.save(ManualEvaluation.builder()
                    .criterionScore(score)
                    .evaluator(currentUser)
                    .evaluationType(request.getEvaluationType())
                    .score(item.getScore())
                    .comment(item.getComment())
                    .build());
        }
        if (request.getEvaluationType() == ManualEvaluationType.SELF) {
            evaluation.setSelfSubmitted(true);
        }
        if (request.getEvaluationType() == ManualEvaluationType.LEADER) {
            evaluation.setLeaderSubmitted(true);
        }
        evaluation.setTotalScore(calculateTotal(evaluation));
        return MemberEvaluationResponse.from(evaluation);
    }

    @Transactional
    public List<MemberEvaluationResponse> finalizeCycle(Long cycleId, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        EvaluationCycle cycle = getCycleAndCheckManager(cycleId, currentUser);
        if (cycle.getStatus() == EvaluationCycleStatus.FINALIZED) {
            throw new BadRequestException("Ky danh gia da duoc chot");
        }
        List<MemberEvaluation> evaluations = memberEvaluationRepository.findByCycleId(cycleId);
        evaluations.forEach(evaluation -> {
            evaluation.setTotalScore(calculateTotal(evaluation));
            evaluation.setFinalized(true);
        });
        cycle.setStatus(EvaluationCycleStatus.FINALIZED);
        cycle.setFinalizedBy(currentUser);
        cycle.setFinalizedAt(LocalDateTime.now());
        return evaluations.stream().map(MemberEvaluationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<EvaluationCycleResponse> getProjectCycles(Long projectId, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        getProjectAndCheckAccess(projectId, currentUser);
        return cycleRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(EvaluationCycleResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MemberEvaluationResponse> getCycleResults(Long cycleId, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        EvaluationCycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay ky danh gia"));
        WorkspaceMember member = getProjectMember(cycle.getProject(), currentUser);
        List<MemberEvaluation> evaluations = memberEvaluationRepository.findByCycleId(cycleId);
        if (member.getRole() == WorkspaceRole.MEMBER && cycle.getStatus() != EvaluationCycleStatus.FINALIZED) {
            return evaluations.stream()
                    .filter(evaluation -> evaluation.getMember().getId().equals(currentUser.getId()))
                    .map(MemberEvaluationResponse::from)
                    .toList();
        }
        return evaluations.stream().map(MemberEvaluationResponse::from).toList();
    }

    public List<String> validateCriteria(List<EvaluationCriterion> criteria) {
        List<EvaluationCriterion> activeCriteria = criteria.stream().filter(EvaluationCriterion::isActive).toList();
        List<String> errors = new ArrayList<>();
        if (activeCriteria.size() < 3 || activeCriteria.size() > 8) {
            errors.add("Moi bo tieu chi can tu 3 den 8 tieu chi");
        }
        int totalWeight = activeCriteria.stream().mapToInt(EvaluationCriterion::getWeight).sum();
        if (totalWeight != 100) {
            errors.add("Tong trong so phai bang 100%");
        }
        Set<String> names = new HashSet<>();
        Set<Integer> sortOrders = new HashSet<>();
        for (EvaluationCriterion criterion : activeCriteria) {
            if (criterion.getName() == null || criterion.getName().isBlank()) {
                errors.add("Ten tieu chi khong duoc de trong");
                continue;
            }
            if (criterion.getWeight() <= 0) {
                errors.add("Trong so tung tieu chi phai lon hon 0");
            }
            if (criterion.getScaleMax() < 5 || criterion.getScaleMax() > 10) {
                errors.add("Thang diem phai tu 5 den 10");
            }
            if (criterion.getSortOrder() < 1 || !sortOrders.add(criterion.getSortOrder())) {
                errors.add("Thu tu tieu chi phai la so duong va khong trung nhau");
            }
            String normalizedName = criterion.getName().trim().toLowerCase(Locale.ROOT);
            if (!names.add(normalizedName)) {
                errors.add("Khong duoc trung ten tieu chi");
            }
            if ((criterion.getEvaluationType() == EvaluationType.AUTO || criterion.getEvaluationType() == EvaluationType.HYBRID)
                    && criterion.getMetricKey() == null) {
                errors.add("AUTO hoac HYBRID phai co metricKey");
            }
            if ((criterion.getEvaluationType() == EvaluationType.MANUAL || criterion.getEvaluationType() == EvaluationType.HYBRID)
                    && criterion.getManualEvaluator() == null) {
                errors.add("MANUAL hoac HYBRID phai co nguoi danh gia");
            }
        }
        return errors;
    }

    public double normalizeManualScore(int score, int scaleMax) {
        if (scaleMax <= 0) {
            throw new BadRequestException("Thang diem khong hop le");
        }
        return Math.min(100.0, Math.max(0.0, score * 100.0 / scaleMax));
    }

    public Double combineScore(EvaluationType type, Double autoScore, Double manualScore, boolean insufficientData) {
        if (type == EvaluationType.AUTO) {
            return insufficientData ? null : autoScore;
        }
        if (type == EvaluationType.MANUAL) {
            return manualScore;
        }
        if (autoScore == null || insufficientData) {
            return manualScore;
        }
        if (manualScore == null) {
            return autoScore;
        }
        return round1(autoScore * 0.4 + manualScore * 0.6);
    }

    public double weightedTotal(List<CriterionScore> scores) {
        double total = scores.stream()
                .filter(score -> score.getFinalScore() != null)
                .mapToDouble(score -> score.getFinalScore() * score.getCriterion().getWeight() / 100.0)
                .sum();
        return round1(total);
    }

    private List<MemberEvaluationResponse> calculateAutomaticScores(Long cycleId, User currentUser) {
        EvaluationCycle cycle = getCycleAndCheckAccess(cycleId, currentUser);
        createMemberEvaluations(cycle);
        List<MemberEvaluation> evaluations = memberEvaluationRepository.findByCycleId(cycleId);
        List<Task> projectTasks = taskRepository.findByProjectIdOrderByCreatedAtDesc(cycle.getProject().getId());
        List<FileEntity> projectFiles = fileRepository.findByProjectIdOrderByCreatedAtDesc(cycle.getProject().getId());
        for (MemberEvaluation evaluation : evaluations) {
            for (CriterionScore score : evaluation.getScores()) {
                MetricResult metric = calculateMetric(score.getCriterion().getMetricKey(), cycle.getProject().getId(), evaluation.getMember().getId(), projectTasks, projectFiles);
                score.setAutoScore(metric.score());
                score.setInsufficientData(metric.insufficientData());
                score.setFinalScore(combineScore(score.getCriterion().getEvaluationType(), score.getAutoScore(), score.getManualScore(), score.isInsufficientData()));
            }
            evaluation.setTotalScore(calculateTotal(evaluation));
        }
        return evaluations.stream().map(MemberEvaluationResponse::from).toList();
    }

    private MetricResult calculateMetric(MetricKey metricKey, Long projectId, Long userId, List<Task> projectTasks, List<FileEntity> projectFiles) {
        if (metricKey == null) {
            return new MetricResult(null, true);
        }
        List<Task> assignedTasks = projectTasks.stream()
                .filter(task -> task.getAssignedTo() != null && task.getAssignedTo().getId().equals(userId))
                .toList();
        return switch (metricKey) {
            case ON_TIME_SUBMISSION -> {
                List<Task> dueTasks = assignedTasks.stream().filter(task -> task.getDueDate() != null).toList();
                if (dueTasks.isEmpty()) yield new MetricResult(null, true);
                long onTime = dueTasks.stream()
                        .filter(task -> task.getStatus() == TaskStatus.DONE
                                && task.getUpdatedAt() != null
                                && !task.getUpdatedAt().toLocalDate().isAfter(task.getDueDate()))
                        .count();
                yield new MetricResult(cap100(onTime * 100.0 / dueTasks.size()), false);
            }
            case TASK_COMPLETION -> {
                int total = assignedTasks.stream().mapToInt(this::difficulty).sum();
                if (total == 0) yield new MetricResult(null, true);
                int done = assignedTasks.stream().filter(task -> task.getStatus() == TaskStatus.DONE).mapToInt(this::difficulty).sum();
                yield new MetricResult(cap100(done * 100.0 / total), false);
            }
            case PROGRESS_UPDATE -> {
                List<Task> activeTasks = assignedTasks.stream().filter(task -> task.getStatus() == TaskStatus.IN_PROGRESS || task.getStatus() == TaskStatus.REVIEW).toList();
                if (activeTasks.isEmpty()) yield new MetricResult(null, true);
                long updated = activeTasks.stream().filter(task -> task.getUpdatedAt() != null && task.getUpdatedAt().isAfter(task.getCreatedAt())).count();
                yield new MetricResult(cap100(updated * 100.0 / activeTasks.size()), false);
            }
            case DOCUMENT_CONTRIBUTION -> {
                long files = projectFiles.stream().filter(file -> file.getUploadedBy().getId().equals(userId)).count();
                if (files == 0) yield new MetricResult(null, true);
                yield new MetricResult(cap100(files * 20.0), false);
            }
            case MEETING_ATTENDANCE -> {
                long meetings = meetingRepository.findByProjectIdOrderByStartTimeDesc(projectId).size();
                if (meetings == 0) yield new MetricResult(null, true);
                long joined = meetingParticipantRepository.countByMeetingProjectIdAndUserId(projectId, userId);
                yield new MetricResult(cap100(joined * 100.0 / meetings), false);
            }
        };
    }

    private void createMemberEvaluations(EvaluationCycle cycle) {
        List<WorkspaceMember> members = workspaceMemberRepository.findByWorkspaceIdOrderByJoinedAtAsc(cycle.getProject().getWorkspace().getId());
        for (WorkspaceMember member : members) {
            MemberEvaluation evaluation = memberEvaluationRepository.findByCycleIdAndMemberId(cycle.getId(), member.getUser().getId())
                    .orElseGet(() -> memberEvaluationRepository.save(MemberEvaluation.builder()
                            .cycle(cycle)
                            .member(member.getUser())
                            .build()));
            for (EvaluationCycleCriterion criterion : cycle.getCriteria()) {
                criterionScoreRepository.findByMemberEvaluationIdAndCriterionId(evaluation.getId(), criterion.getId())
                        .orElseGet(() -> {
                            CriterionScore score = CriterionScore.builder()
                                    .memberEvaluation(evaluation)
                                    .criterion(criterion)
                                    .insufficientData(false)
                                    .build();
                            evaluation.getScores().add(score);
                            return criterionScoreRepository.save(score);
                        });
            }
        }
    }

    private double calculateTotal(MemberEvaluation evaluation) {
        return weightedTotal(evaluation.getScores());
    }

    private void ensureCycleEditable(EvaluationCycle cycle) {
        if (cycle.getStatus() == EvaluationCycleStatus.FINALIZED) {
            throw new BadRequestException("Khong the sua ky da chot");
        }
    }

    private User resolveManualTarget(ManualScoreRequest request, User currentUser, EvaluationCycle cycle) {
        Long memberUserId = request.getMemberUserId() == null ? currentUser.getId() : request.getMemberUserId();
        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(cycle.getProject().getWorkspace().getId(), memberUserId)) {
            throw new ForbiddenException("Thanh vien khong thuoc workspace");
        }
        return userRepository.findById(memberUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay thanh vien"));
    }

    private ProjectEvaluationConfig ensureConfig(Project project, User currentUser) {
        return configRepository.findByProjectId(project.getId())
                .orElseGet(() -> {
                    EvaluationTemplate template = templateRepository.findByLevelAndSystemTemplate(TemplateLevel.BASIC, true)
                            .or(() -> templateRepository.findBySystemTemplateTrueOrderByIdAsc().stream().findFirst())
                            .orElseThrow(() -> new BadRequestException("Chua co mau danh gia mac dinh"));
                    ProjectEvaluationConfig config = ProjectEvaluationConfig.builder()
                            .project(project)
                            .sourceTemplate(template)
                            .name(template.getName())
                            .updatedBy(currentUser)
                            .build();
                    template.getCriteria().stream()
                            .sorted((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()))
                            .forEach(source -> config.getCriteria().add(copyCriterionToConfig(source, config)));
                    return configRepository.save(config);
                });
    }

    private EvaluationCriterion buildCriterion(EvaluationCriterionRequest request, ProjectEvaluationConfig config) {
        return EvaluationCriterion.builder()
                .projectConfig(config)
                .name(request.getName())
                .description(request.getDescription())
                .weight(request.getWeight())
                .evaluationType(request.getEvaluationType())
                .metricKey(request.getMetricKey())
                .scaleMax(request.getScaleMax())
                .manualEvaluator(request.getManualEvaluator())
                .requiresEvidence(request.isRequiresEvidence())
                .requiresComment(request.isRequiresComment())
                .sortOrder(request.getSortOrder())
                .active(request.isActive())
                .build();
    }

    private EvaluationCriterion copyCriterionToConfig(EvaluationCriterion source, ProjectEvaluationConfig config) {
        return EvaluationCriterion.builder()
                .projectConfig(config)
                .name(source.getName())
                .description(source.getDescription())
                .weight(source.getWeight())
                .evaluationType(source.getEvaluationType())
                .metricKey(source.getMetricKey())
                .scaleMax(source.getScaleMax())
                .manualEvaluator(source.getManualEvaluator())
                .requiresEvidence(source.isRequiresEvidence())
                .requiresComment(source.isRequiresComment())
                .sortOrder(source.getSortOrder())
                .active(source.isActive())
                .build();
    }

    private EvaluationCriterion copyCriterionToTemplate(EvaluationCriterion source, EvaluationTemplate template) {
        EvaluationCriterion copy = copyCriterionToConfig(source, null);
        copy.setTemplate(template);
        copy.setProjectConfig(null);
        return copy;
    }

    private EvaluationCycleCriterion copyCriterionToCycle(EvaluationCriterion source, EvaluationCycle cycle) {
        return EvaluationCycleCriterion.builder()
                .cycle(cycle)
                .name(source.getName())
                .description(source.getDescription())
                .weight(source.getWeight())
                .evaluationType(source.getEvaluationType())
                .metricKey(source.getMetricKey())
                .scaleMax(source.getScaleMax())
                .manualEvaluator(source.getManualEvaluator())
                .requiresEvidence(source.isRequiresEvidence())
                .requiresComment(source.isRequiresComment())
                .sortOrder(source.getSortOrder())
                .build();
    }

    private Project getProjectAndCheckAccess(Long projectId, User currentUser) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay project"));
        getProjectMember(project, currentUser);
        return project;
    }

    private Project getProjectAndCheckManager(Long projectId, User currentUser) {
        Project project = getProjectAndCheckAccess(projectId, currentUser);
        WorkspaceMember member = getProjectMember(project, currentUser);
        if (member.getRole() != WorkspaceRole.OWNER && member.getRole() != WorkspaceRole.LEADER) {
            throw new ForbiddenException("Chi OWNER hoac LEADER moi duoc thuc hien hanh dong nay");
        }
        return project;
    }

    private void lockProject(Long projectId) {
        projectRepository.findByIdForUpdate(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay project"));
    }

    private EvaluationCycle getCycleAndCheckAccess(Long cycleId, User currentUser) {
        EvaluationCycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay ky danh gia"));
        getProjectMember(cycle.getProject(), currentUser);
        return cycle;
    }

    private EvaluationCycle getCycleAndCheckManager(Long cycleId, User currentUser) {
        EvaluationCycle cycle = getCycleAndCheckAccess(cycleId, currentUser);
        WorkspaceMember member = getProjectMember(cycle.getProject(), currentUser);
        if (member.getRole() != WorkspaceRole.OWNER && member.getRole() != WorkspaceRole.LEADER) {
            throw new ForbiddenException("Chi OWNER hoac LEADER moi duoc thuc hien hanh dong nay");
        }
        return cycle;
    }

    private WorkspaceMember getProjectMember(Project project, User currentUser) {
        return workspaceMemberRepository.findByWorkspaceIdAndUserId(project.getWorkspace().getId(), currentUser.getId())
                .orElseThrow(() -> new ForbiddenException("Ban khong co quyen truy cap workspace nay"));
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new UnauthorizedException("Ban chua dang nhap");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UnauthorizedException("Khong tim thay user hien tai"));
    }

    private int difficulty(Task task) {
        TaskPriority priority = task.getPriority();
        if (priority == TaskPriority.URGENT) return 5;
        if (priority == TaskPriority.HIGH) return 4;
        if (priority == TaskPriority.MEDIUM) return 3;
        return 2;
    }

    private double cap100(double value) {
        return round1(Math.min(100.0, Math.max(0.0, value)));
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private record MetricResult(Double score, boolean insufficientData) {
    }
}
