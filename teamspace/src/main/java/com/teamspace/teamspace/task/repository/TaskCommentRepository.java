package com.teamspace.teamspace.task.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamspace.teamspace.task.entity.TaskComment;

public interface TaskCommentRepository extends JpaRepository<TaskComment, Long> {

    List<TaskComment> findByTaskIdOrderByCreatedAtAsc(Long taskId);

    List<TaskComment> findByTaskProjectId(Long projectId);
}
