package com.teamspace.teamspace.file.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamspace.teamspace.file.entity.FileEntity;

public interface FileRepository extends JpaRepository<FileEntity, Long> {

    List<FileEntity> findByProjectIdAndTaskIsNullOrderByCreatedAtDesc(Long projectId);

    List<FileEntity> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    List<FileEntity> findByTaskIdOrderByCreatedAtDesc(Long taskId);
}
