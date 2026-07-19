package com.teamspace.teamspace.workcategory.service;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamspace.teamspace.exception.ForbiddenException;
import com.teamspace.teamspace.exception.ResourceNotFoundException;
import com.teamspace.teamspace.exception.UnauthorizedException;
import com.teamspace.teamspace.project.entity.Project;
import com.teamspace.teamspace.project.repository.ProjectRepository;
import com.teamspace.teamspace.realtime.service.ProjectRealtimeService;
import com.teamspace.teamspace.task.repository.TaskRepository;
import com.teamspace.teamspace.user.entity.User;
import com.teamspace.teamspace.user.repository.UserRepository;
import com.teamspace.teamspace.workcategory.dto.WorkCategoryOrderRequest;
import com.teamspace.teamspace.workcategory.dto.WorkCategoryRequest;
import com.teamspace.teamspace.workcategory.dto.WorkCategoryResponse;
import com.teamspace.teamspace.workcategory.entity.WorkCategory;
import com.teamspace.teamspace.workcategory.repository.WorkCategoryRepository;
import com.teamspace.teamspace.workspace.entity.WorkspaceMember;
import com.teamspace.teamspace.workspace.enums.WorkspaceRole;
import com.teamspace.teamspace.workspace.repository.WorkspaceMemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkCategoryService {
    private static final String[][] DEFAULTS = {
            {"Nghiên cứu", "#2563EB", "Search"},
            {"Viết nội dung", "#16A34A", "FileText"},
            {"Thiết kế", "#7C3AED", "Palette"},
            {"Lập trình", "#0891B2", "Code2"},
            {"Thuyết trình", "#EA580C", "Presentation"},
            {"Kiểm tra/chỉnh sửa", "#DC2626", "CheckCircle2"}
    };

    private final WorkCategoryRepository repository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final ProjectRealtimeService realtime;

    @Transactional(readOnly = true)
    public List<WorkCategoryResponse> list(Long projectId, Authentication authentication) {
        Project project = project(projectId);
        member(project, user(authentication));
        return repository.findByProjectIdOrderByPositionAsc(projectId).stream()
                .map(WorkCategoryResponse::from)
                .toList();
    }

    @Transactional
    public WorkCategoryResponse create(Long projectId, WorkCategoryRequest request, Authentication authentication) {
        Project project = project(projectId);
        User creator = user(authentication);
        requireManager(member(project, creator));
        String normalizedName = normalize(request.getName());
        if (repository.findByProjectIdAndNormalizedName(projectId, normalizedName).isPresent()) {
            throw new IllegalArgumentException("Tên lĩnh vực đã tồn tại trong dự án");
        }
        WorkCategory category = repository.save(WorkCategory.builder()
                .project(project)
                .name(request.getName().trim())
                .normalizedName(normalizedName)
                .color(request.getColor())
                .icon(request.getIcon())
                .position(repository.findByProjectIdOrderByPositionAsc(projectId).size())
                .active(true)
                .createdBy(creator)
                .build());
        publish(category);
        return WorkCategoryResponse.from(category);
    }

    @Transactional
    public WorkCategoryResponse update(Long categoryId, WorkCategoryRequest request, Authentication authentication) {
        WorkCategory category = category(categoryId);
        requireManager(member(category.getProject(), user(authentication)));
        if (request.getVersion() != null && !Objects.equals(request.getVersion(), category.getVersion())) {
            throw new IllegalArgumentException("Lĩnh vực đã được cập nhật bởi người khác");
        }
        String normalizedName = normalize(request.getName());
        if (repository.existsByProjectIdAndNormalizedNameAndIdNot(
                category.getProject().getId(), normalizedName, categoryId)) {
            throw new IllegalArgumentException("Tên lĩnh vực đã tồn tại trong dự án");
        }
        category.setName(request.getName().trim());
        category.setNormalizedName(normalizedName);
        category.setColor(request.getColor());
        category.setIcon(request.getIcon());
        publish(category);
        return WorkCategoryResponse.from(category);
    }

    @Transactional
    public List<WorkCategoryResponse> order(Long projectId, WorkCategoryOrderRequest request,
            Authentication authentication) {
        Project project = project(projectId);
        requireManager(member(project, user(authentication)));
        List<WorkCategory> categories = repository.findByProjectIdOrderByPositionAsc(projectId);
        Set<Long> currentIds = categories.stream().map(WorkCategory::getId).collect(Collectors.toSet());
        Set<Long> requestedIds = new HashSet<>(request.getCategoryIds());
        if (request.getCategoryIds().size() != categories.size()
                || requestedIds.size() != categories.size()
                || !currentIds.equals(requestedIds)) {
            throw new IllegalArgumentException("Danh sách lĩnh vực không hợp lệ");
        }
        Map<Long, WorkCategory> byId = categories.stream()
                .collect(Collectors.toMap(WorkCategory::getId, category -> category));
        for (int position = 0; position < request.getCategoryIds().size(); position++) {
            byId.get(request.getCategoryIds().get(position)).setPosition(position);
        }
        realtime.publish(projectId, null, "WORK_CATEGORY_UPDATED", null, Map.of());
        return request.getCategoryIds().stream().map(byId::get).map(WorkCategoryResponse::from).toList();
    }

    @Transactional
    public WorkCategoryResponse active(Long categoryId, boolean active, Authentication authentication) {
        WorkCategory category = category(categoryId);
        requireManager(member(category.getProject(), user(authentication)));
        category.setActive(active);
        publish(category);
        return WorkCategoryResponse.from(category);
    }

    @Transactional(readOnly = true)
    public WorkCategory resolve(Project project, Long categoryId, String legacyType) {
        if (categoryId != null) {
            WorkCategory category = category(categoryId);
            if (!Objects.equals(category.getProject().getId(), project.getId())) {
                throw new IllegalArgumentException("Lĩnh vực không thuộc dự án");
            }
            if (!category.isActive()) throw new IllegalArgumentException("Lĩnh vực đã ngừng sử dụng");
            return category;
        }
        String normalizedName = normalize(legacyType == null ? "Thiết kế" : legacyType);
        return repository.findByProjectIdAndNormalizedName(project.getId(), normalizedName)
                .orElseGet(() -> repository.findByProjectIdOrderByPositionAsc(project.getId()).stream()
                        .filter(WorkCategory::isActive)
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Dự án chưa có lĩnh vực công việc")));
    }

    @Transactional
    public void initializeDefaults(Project project, User creator) {
        if (!repository.findByProjectIdOrderByPositionAsc(project.getId()).isEmpty()) return;
        for (int position = 0; position < DEFAULTS.length; position++) {
            repository.save(WorkCategory.builder()
                    .project(project)
                    .name(DEFAULTS[position][0])
                    .normalizedName(normalize(DEFAULTS[position][0]))
                    .color(DEFAULTS[position][1])
                    .icon(DEFAULTS[position][2])
                    .position(position)
                    .active(true)
                    .createdBy(creator)
                    .build());
        }
    }

    public static String normalize(String value) {
        return Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFC);
    }

    private void publish(WorkCategory category) {
        realtime.publish(category.getProject().getId(), null, "WORK_CATEGORY_UPDATED", category.getVersion(),
                Map.of("categoryId", category.getId()));
    }

    private WorkCategory category(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lĩnh vực"));
    }

    private Project project(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dự án"));
    }

    private User user(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new UnauthorizedException("Bạn chưa đăng nhập");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy người dùng"));
    }

    private WorkspaceMember member(Project project, User user) {
        return memberRepository.findByWorkspaceIdAndUserId(project.getWorkspace().getId(), user.getId())
                .orElseThrow(() -> new ForbiddenException("Bạn không thuộc dự án"));
    }

    private void requireManager(WorkspaceMember member) {
        if (member.getRole() != WorkspaceRole.OWNER && member.getRole() != WorkspaceRole.LEADER) {
            throw new ForbiddenException("Chỉ OWNER hoặc LEADER được quản lý lĩnh vực");
        }
    }
}
