package com.teamspace.teamspace.evaluation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.teamspace.teamspace.evaluation.entity.EvaluationEvidence;

public interface EvaluationEvidenceRepository extends JpaRepository<EvaluationEvidence, Long> {
    @Modifying(flushAutomatically = true)
    @Query("""
            delete from EvaluationEvidence evidence
            where evidence.task.id = :taskId
               or evidence.file.id in (
                    select file.id from FileEntity file where file.task.id = :taskId
               )
            """)
    void deleteByTaskId(@Param("taskId") Long taskId);

    @Modifying(flushAutomatically = true)
    @Query("delete from EvaluationEvidence evidence where evidence.file.id = :fileId")
    void deleteByFileId(@Param("fileId") Long fileId);
}
