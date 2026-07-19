package com.teamspace.teamspace.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.teamspace.teamspace.evaluation.repository.EvaluationEvidenceRepository;
import com.teamspace.teamspace.file.entity.FileEntity;
import com.teamspace.teamspace.file.service.FileService;
import com.teamspace.teamspace.task.entity.Task;
import com.teamspace.teamspace.task.entity.TaskChecklistItem;
import com.teamspace.teamspace.task.entity.TaskComment;
import com.teamspace.teamspace.task.entity.TaskReviewHistory;
import com.teamspace.teamspace.task.repository.TaskWatcherRepository;
import com.teamspace.teamspace.task.service.TaskDeletionService;
import com.teamspace.teamspace.taskactivity.repository.TaskActivityRepository;

@ExtendWith(MockitoExtension.class)
class TaskDeletionServiceTest {
    @Mock EvaluationEvidenceRepository evidenceRepository;
    @Mock TaskWatcherRepository watcherRepository;
    @Mock TaskActivityRepository activityRepository;
    @Mock FileService fileService;
    @InjectMocks TaskDeletionService service;

    @Test
    void removesExternalReferencesAndClearsCascadeCollections() {
        Task task = Task.builder().id(80L).build();
        task.getFiles().add(FileEntity.builder().id(1L).task(task).build());
        task.getComments().add(TaskComment.builder().id(2L).task(task).build());
        task.getChecklistItems().add(TaskChecklistItem.builder().id(3L).task(task).build());
        task.getReviewHistory().add(TaskReviewHistory.builder().id(4L).task(task).build());

        service.cleanup(task);

        verify(evidenceRepository).deleteByTaskId(task.getId());
        verify(watcherRepository).deleteByTaskId(task.getId());
        verify(activityRepository).deleteByTaskId(task.getId());
        verify(fileService).scheduleTaskFilesDeletion(task.getId());
        assertThat(task.getFiles()).isEmpty();
        assertThat(task.getComments()).isEmpty();
        assertThat(task.getChecklistItems()).isEmpty();
        assertThat(task.getReviewHistory()).isEmpty();
    }
}
