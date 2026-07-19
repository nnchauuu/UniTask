package com.teamspace.teamspace.task.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import com.teamspace.teamspace.project.entity.Project;
import com.teamspace.teamspace.file.entity.FileEntity;
import com.teamspace.teamspace.task.enums.TaskPriority;
import com.teamspace.teamspace.task.enums.TaskStatus;
import com.teamspace.teamspace.user.entity.User;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.teamspace.teamspace.task.enums.TaskReviewStatus;
import com.teamspace.teamspace.planning.entity.WeeklyPlan;
import com.teamspace.teamspace.planning.enums.PlanningState;
import com.teamspace.teamspace.workcategory.entity.WorkCategory;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private User assignedTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_task_id")
    private Task parentTask;

    @Builder.Default
    @OneToMany(mappedBy = "parentTask")
    private Set<Task> subtasks = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_column_id")
    private BoardColumn boardColumn;

    private Long boardPosition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlanningState planningState;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "weekly_plan_id")
    private WeeklyPlan weeklyPlan;

    private Long backlogPosition;

    @Version
    private Long version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TaskPriority priority;

    @Column(nullable = false, length = 100)
    private String type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_category_id", nullable = false)
    private WorkCategory workCategory;

    @Column(nullable = false)
    private boolean reviewRequired;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TaskReviewStatus reviewStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private User reviewer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by_id")
    private User submittedBy;

    private LocalDateTime submittedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private User reviewedBy;

    private LocalDateTime reviewedAt;

    private LocalDate startDate;

    private LocalDate dueDate;

    @Column(length = 500)
    private String labels;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal estimatedEffort;
    @Column(precision = 10, scale = 2)
    private BigDecimal actualEffort;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @Builder.Default
    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TaskComment> comments = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TaskChecklistItem> checklistItems = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TaskReviewHistory> reviewHistory = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<FileEntity> files = new HashSet<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;

        if (status == null) {
            status = TaskStatus.TODO;
        }

        if (priority == null) {
            priority = TaskPriority.MEDIUM;
        }

        if (type == null || type.isBlank()) {
            type = "DESIGN";
        }

        if (reviewStatus == null) {
            reviewStatus = TaskReviewStatus.NONE;
        }

        if (planningState == null) {
            planningState = PlanningState.ACTIVE;
        }
        if (estimatedEffort == null) estimatedEffort = BigDecimal.ZERO;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
