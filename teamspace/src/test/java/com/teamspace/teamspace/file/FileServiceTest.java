package com.teamspace.teamspace.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.teamspace.teamspace.activity.service.ActivityLogService;
import com.teamspace.teamspace.evaluation.repository.EvaluationEvidenceRepository;
import com.teamspace.teamspace.file.entity.FileEntity;
import com.teamspace.teamspace.file.repository.FileRepository;
import com.teamspace.teamspace.file.service.FileService;
import com.teamspace.teamspace.notification.service.NotificationService;
import com.teamspace.teamspace.project.repository.ProjectRepository;
import com.teamspace.teamspace.realtime.service.TaskChangeCoordinator;
import com.teamspace.teamspace.task.repository.TaskRepository;
import com.teamspace.teamspace.user.repository.UserRepository;
import com.teamspace.teamspace.workspace.repository.WorkspaceMemberRepository;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {
    @Mock FileRepository fileRepository;
    @Mock ProjectRepository projectRepository;
    @Mock TaskRepository taskRepository;
    @Mock WorkspaceMemberRepository workspaceMemberRepository;
    @Mock UserRepository userRepository;
    @Mock NotificationService notificationService;
    @Mock ActivityLogService activityLogService;
    @Mock TaskChangeCoordinator taskChanges;
    @Mock EvaluationEvidenceRepository evaluationEvidenceRepository;
    @InjectMocks FileService service;

    @Test
    void removesPhysicalTaskFilesWhenNoTransactionIsActive(@TempDir Path tempDir) throws Exception {
        Path storedFile = Files.writeString(tempDir.resolve("task-file.txt"), "content");
        when(fileRepository.findByTaskIdOrderByCreatedAtDesc(90L)).thenReturn(List.of(
                FileEntity.builder().id(1L).filePath(storedFile.toString()).build()
        ));

        service.scheduleTaskFilesDeletion(90L);

        assertThat(storedFile).doesNotExist();
    }
}
