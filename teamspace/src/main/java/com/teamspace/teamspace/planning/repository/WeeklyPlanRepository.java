package com.teamspace.teamspace.planning.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.teamspace.teamspace.planning.entity.WeeklyPlan;
import com.teamspace.teamspace.planning.enums.WeeklyPlanStatus;

import jakarta.persistence.LockModeType;

public interface WeeklyPlanRepository extends JpaRepository<WeeklyPlan, Long> {
    List<WeeklyPlan> findByProjectIdOrderByStartDateDescCreatedAtDesc(Long projectId);
    Optional<WeeklyPlan> findByProjectIdAndStatus(Long projectId, WeeklyPlanStatus status);
    List<WeeklyPlan> findByEndDateAndStatus(LocalDate endDate, WeeklyPlanStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select plan from WeeklyPlan plan where plan.id = :id")
    Optional<WeeklyPlan> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select plan from WeeklyPlan plan where plan.project.id = :projectId and plan.status = com.teamspace.teamspace.planning.enums.WeeklyPlanStatus.ACTIVE")
    Optional<WeeklyPlan> findActiveByProjectIdForUpdate(@Param("projectId") Long projectId);
}
