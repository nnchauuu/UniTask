package com.teamspace.teamspace.task.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamspace.teamspace.task.entity.BoardColumn;
import com.teamspace.teamspace.task.enums.StatusGroup;

public interface BoardColumnRepository extends JpaRepository<BoardColumn, Long> {

    List<BoardColumn> findByProjectIdOrderByPositionAsc(Long projectId);

    Optional<BoardColumn> findByProjectIdAndKey(Long projectId, String key);

    long countByProjectId(Long projectId);

    Optional<BoardColumn> findByProjectIdAndStatusGroupAndDefaultForGroupTrue(Long projectId, StatusGroup statusGroup);
}
