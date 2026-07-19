package com.teamspace.teamspace.taskactivity.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.teamspace.teamspace.taskactivity.entity.TaskActivity;

public interface TaskActivityRepository extends JpaRepository<TaskActivity, Long> {
    Page<TaskActivity> findByTaskId(Long taskId, Pageable pageable);
    @Modifying(flushAutomatically = true)
    @Query("delete from TaskActivity activity where activity.task.id = :taskId")
    void deleteByTaskId(@Param("taskId") Long taskId);
}
