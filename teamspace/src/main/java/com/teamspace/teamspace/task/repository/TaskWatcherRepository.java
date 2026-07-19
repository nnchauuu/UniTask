package com.teamspace.teamspace.task.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.teamspace.teamspace.task.entity.TaskWatcher;

public interface TaskWatcherRepository extends JpaRepository<TaskWatcher, Long> {
    List<TaskWatcher> findByTaskIdOrderByCreatedAtAsc(Long taskId);
    Optional<TaskWatcher> findByTaskIdAndUserId(Long taskId, Long userId);
    boolean existsByTaskIdAndUserId(Long taskId, Long userId);
    @Modifying(flushAutomatically = true)
    @Query("delete from TaskWatcher watcher where watcher.task.id = :taskId")
    void deleteByTaskId(@Param("taskId") Long taskId);
}
