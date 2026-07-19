package com.teamspace.teamspace.planning.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import com.teamspace.teamspace.planning.enums.WeeklyPlanStatus;
import com.teamspace.teamspace.planning.enums.EstimateUnit;
import com.teamspace.teamspace.project.entity.Project;
import com.teamspace.teamspace.task.entity.Task;
import com.teamspace.teamspace.user.entity.User;

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
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "weekly_plans", uniqueConstraints = @UniqueConstraint(
        name = "uk_weekly_plan_active_project", columnNames = "active_project_key"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 1000)
    private String goal;

    @Column(length = 2000)
    private String description;

    @Column(precision = 10, scale = 2)
    private BigDecimal capacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstimateUnit estimateUnit;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WeeklyPlanStatus status;

    @Column(name = "active_project_key")
    private Long activeProjectKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @Builder.Default
    @OneToMany(mappedBy = "weeklyPlan")
    private Set<Task> tasks = new HashSet<>();

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "started_by_id")
    private User startedBy;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "completed_by_id")
    private User completedBy;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "cancelled_by_id")
    private User cancelledBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    public void changeStatus(WeeklyPlanStatus nextStatus) {
        status = nextStatus;
        activeProjectKey = nextStatus == WeeklyPlanStatus.ACTIVE ? project.getId() : null;
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) status = WeeklyPlanStatus.DRAFT;
        if (estimateUnit == null) estimateUnit = EstimateUnit.HOURS;
        activeProjectKey = status == WeeklyPlanStatus.ACTIVE ? project.getId() : null;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
