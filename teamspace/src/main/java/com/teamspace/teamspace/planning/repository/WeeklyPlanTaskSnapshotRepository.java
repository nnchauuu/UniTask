package com.teamspace.teamspace.planning.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamspace.teamspace.planning.entity.WeeklyPlanTaskSnapshot;

public interface WeeklyPlanTaskSnapshotRepository extends JpaRepository<WeeklyPlanTaskSnapshot, Long> {
    List<WeeklyPlanTaskSnapshot> findByWeeklyPlanIdOrderBySortOrderAsc(Long weeklyPlanId);
    boolean existsByWeeklyPlanId(Long weeklyPlanId);
}
