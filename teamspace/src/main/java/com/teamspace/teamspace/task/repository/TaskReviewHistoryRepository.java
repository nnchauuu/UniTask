package com.teamspace.teamspace.task.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamspace.teamspace.task.entity.TaskReviewHistory;

public interface TaskReviewHistoryRepository extends JpaRepository<TaskReviewHistory, Long> {
    List<TaskReviewHistory> findByTaskIdOrderByCreatedAtAscIdAsc(Long taskId);
}
