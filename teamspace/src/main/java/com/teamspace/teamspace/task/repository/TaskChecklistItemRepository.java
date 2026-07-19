package com.teamspace.teamspace.task.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamspace.teamspace.task.entity.TaskChecklistItem;

public interface TaskChecklistItemRepository extends JpaRepository<TaskChecklistItem, Long> {
    List<TaskChecklistItem> findByTaskIdOrderByPositionAscIdAsc(Long taskId);
    long countByTaskId(Long taskId);
}
