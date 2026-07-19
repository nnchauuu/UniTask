package com.teamspace.teamspace.task.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamspace.teamspace.evaluation.repository.EvaluationEvidenceRepository;
import com.teamspace.teamspace.file.service.FileService;
import com.teamspace.teamspace.task.entity.Task;
import com.teamspace.teamspace.task.repository.TaskWatcherRepository;
import com.teamspace.teamspace.taskactivity.repository.TaskActivityRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TaskDeletionService {
    private final EvaluationEvidenceRepository evidenceRepository;
    private final TaskWatcherRepository watcherRepository;
    private final TaskActivityRepository activityRepository;
    private final FileService fileService;

    @Transactional
    public void cleanup(Task task) {
        Long taskId = task.getId();

        // Evidence can point to both the task and one of its files, so remove it first.
        evidenceRepository.deleteByTaskId(taskId);
        watcherRepository.deleteByTaskId(taskId);
        activityRepository.deleteByTaskId(taskId);

        // Database file rows, comments, checklist and review history are orphan-removal
        // collections on Task. Physical files are removed only after the transaction commits.
        fileService.scheduleTaskFilesDeletion(taskId);
        task.getFiles().clear();
        task.getComments().clear();
        task.getChecklistItems().clear();
        task.getReviewHistory().clear();
    }
}
