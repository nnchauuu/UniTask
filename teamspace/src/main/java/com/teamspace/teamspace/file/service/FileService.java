package com.teamspace.teamspace.file.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.teamspace.teamspace.activity.enums.ActivityAction;
import com.teamspace.teamspace.activity.service.ActivityLogService;
import com.teamspace.teamspace.evaluation.repository.EvaluationEvidenceRepository;
import com.teamspace.teamspace.realtime.service.TaskChangeCoordinator;
import com.teamspace.teamspace.exception.BadRequestException;
import com.teamspace.teamspace.exception.ForbiddenException;
import com.teamspace.teamspace.exception.ResourceNotFoundException;
import com.teamspace.teamspace.exception.UnauthorizedException;
import com.teamspace.teamspace.file.dto.FileResponse;
import com.teamspace.teamspace.file.entity.FileEntity;
import com.teamspace.teamspace.file.repository.FileRepository;
import com.teamspace.teamspace.notification.enums.NotificationType;
import com.teamspace.teamspace.notification.service.NotificationService;
import com.teamspace.teamspace.project.entity.Project;
import com.teamspace.teamspace.project.repository.ProjectRepository;
import com.teamspace.teamspace.task.entity.Task;
import com.teamspace.teamspace.task.repository.TaskRepository;
import com.teamspace.teamspace.user.entity.User;
import com.teamspace.teamspace.user.repository.UserRepository;
import com.teamspace.teamspace.workspace.entity.WorkspaceMember;
import com.teamspace.teamspace.workspace.enums.WorkspaceRole;
import com.teamspace.teamspace.workspace.repository.WorkspaceMemberRepository;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {

    private final FileRepository fileRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ActivityLogService activityLogService;
    private final TaskChangeCoordinator taskChanges;
    private final EvaluationEvidenceRepository evaluationEvidenceRepository;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @Transactional
    public FileResponse uploadProjectFile(Long projectId, MultipartFile file, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        Project project = getProjectOrThrow(projectId);
        getCurrentMemberOrThrow(project.getWorkspace().getId(), currentUser.getId());

        FileEntity savedFile = storeFile(project, null, currentUser, file);
        activityLogService.log(
                project,
                currentUser,
                ActivityAction.FILE_UPLOADED,
                "FILE",
                savedFile.getId(),
                currentUser.getFullName() + " uploaded file: " + savedFile.getOriginalName()
        );
        notifyFileUploaded(project, savedFile, currentUser);
        return FileResponse.from(savedFile);
    }

    @Transactional
    public FileResponse uploadTaskFile(Long taskId, MultipartFile file, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        Task task = getTaskOrThrow(taskId);
        Project project = task.getProject();
        getCurrentMemberOrThrow(project.getWorkspace().getId(), currentUser.getId());

        FileEntity savedFile = storeFile(project, task, currentUser, file);
        activityLogService.log(
                project,
                currentUser,
                ActivityAction.FILE_UPLOADED,
                "FILE",
                savedFile.getId(),
                currentUser.getFullName() + " uploaded file: " + savedFile.getOriginalName()
        );
        notifyFileUploaded(project, savedFile, currentUser);
        taskChanges.changed(task, currentUser, "TASK_UPDATED", "attachment", null, savedFile.getOriginalName(), currentUser.getFullName() + " thêm tệp " + savedFile.getOriginalName(), true);
        return FileResponse.from(savedFile);
    }

    @Transactional(readOnly = true)
    public List<FileResponse> getProjectFiles(Long projectId, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        Project project = getProjectOrThrow(projectId);
        getCurrentMemberOrThrow(project.getWorkspace().getId(), currentUser.getId());

        return fileRepository.findByProjectIdAndTaskIsNullOrderByCreatedAtDesc(projectId)
                .stream()
                .map(FileResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FileResponse> getTaskFiles(Long taskId, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        Task task = getTaskOrThrow(taskId);
        getCurrentMemberOrThrow(task.getProject().getWorkspace().getId(), currentUser.getId());

        return fileRepository.findByTaskIdOrderByCreatedAtDesc(taskId)
                .stream()
                .map(FileResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DownloadFile getDownloadFile(Long fileId, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        FileEntity file = getFileOrThrow(fileId);
        getCurrentMemberOrThrow(file.getProject().getWorkspace().getId(), currentUser.getId());

        Path filePath = Paths.get(file.getFilePath()).normalize();
        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResourceNotFoundException("Khong tim thay file tren server");
            }

            return new DownloadFile(file, resource);
        } catch (MalformedURLException exception) {
            throw new ResourceNotFoundException("Khong tim thay file tren server");
        }
    }

    @Transactional
    public void deleteFile(Long fileId, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        FileEntity file = getFileOrThrow(fileId);
        WorkspaceMember currentMember = getCurrentMemberOrThrow(file.getProject().getWorkspace().getId(), currentUser.getId());

        if (!canDeleteFile(currentMember, file, currentUser)) {
            throw new ForbiddenException("Chi OWNER, LEADER hoac nguoi tai len moi duoc xoa tep");
        }

        Path filePath = Paths.get(file.getFilePath()).normalize();
        activityLogService.log(
                file.getProject(),
                currentUser,
                ActivityAction.FILE_DELETED,
                "FILE",
                file.getId(),
                currentUser.getFullName() + " deleted file: " + file.getOriginalName()
        );
        if (file.getTask() != null) taskChanges.changed(file.getTask(), currentUser, "TASK_UPDATED", "attachment", file.getOriginalName(), null, currentUser.getFullName() + " xóa tệp " + file.getOriginalName(), true);
        evaluationEvidenceRepository.deleteByFileId(file.getId());
        fileRepository.delete(file);
        schedulePhysicalDeletion(List.of(filePath));
    }

    @Transactional(readOnly = true)
    public void scheduleTaskFilesDeletion(Long taskId) {
        List<Path> paths = fileRepository.findByTaskIdOrderByCreatedAtDesc(taskId).stream()
                .map(FileEntity::getFilePath)
                .filter(path -> path != null && !path.isBlank())
                .map(path -> Paths.get(path).normalize())
                .toList();
        if (paths.isEmpty()) return;

        schedulePhysicalDeletion(paths);
    }

    private void schedulePhysicalDeletion(List<Path> paths) {
        Runnable cleanup = () -> paths.forEach(this::deletePhysicalFileQuietly);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    cleanup.run();
                }
            });
        } else {
            cleanup.run();
        }
    }

    private void deletePhysicalFileQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            path.toFile().deleteOnExit();
            log.error("Khong the xoa tep vat ly sau khi xoa task: {}", path, exception);
        }
    }

    private FileEntity storeFile(Project project, Task task, User uploadedBy, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Tep khong duoc rong");
        }

        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        if (originalName.contains("..")) {
            throw new BadRequestException("Ten tep khong hop le");
        }

        String extension = "";
        int extensionIndex = originalName.lastIndexOf('.');
        if (extensionIndex >= 0) {
            extension = originalName.substring(extensionIndex);
        }

        String storedName = UUID.randomUUID() + extension;
        Path uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path targetPath = uploadRoot.resolve(storedName).normalize();

        if (!targetPath.startsWith(uploadRoot)) {
            throw new BadRequestException("Duong dan file khong hop le");
        }

        try {
            Files.createDirectories(uploadRoot);
            file.transferTo(targetPath);
        } catch (IOException exception) {
            throw new BadRequestException("Khong the luu file");
        }

        FileEntity fileEntity = FileEntity.builder()
                .project(project)
                .task(task)
                .uploadedBy(uploadedBy)
                .originalName(originalName)
                .storedName(storedName)
                .filePath(targetPath.toString())
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .build();

        return fileRepository.save(fileEntity);
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new UnauthorizedException("Ban chua dang nhap");
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UnauthorizedException("Khong tim thay user hien tai"));
    }

    private Project getProjectOrThrow(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay project"));
    }

    private Task getTaskOrThrow(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay task"));
    }

    private FileEntity getFileOrThrow(Long fileId) {
        return fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay file"));
    }

    private WorkspaceMember getCurrentMemberOrThrow(Long workspaceId, Long userId) {
        return workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new ForbiddenException("Ban khong co quyen truy cap workspace nay"));
    }

    private boolean canDeleteFile(WorkspaceMember member, FileEntity file, User currentUser) {
        return member.getRole() == WorkspaceRole.OWNER
                || member.getRole() == WorkspaceRole.LEADER
                || file.getUploadedBy().getId().equals(currentUser.getId());
    }

    private void notifyFileUploaded(Project project, FileEntity file, User actor) {
        workspaceMemberRepository.findByWorkspaceIdOrderByJoinedAtAsc(project.getWorkspace().getId())
                .stream()
                .map(WorkspaceMember::getUser)
                .filter(user -> !user.getId().equals(actor.getId()))
                .forEach(user -> notificationService.createAndSend(
                        user,
                        "Tep moi da duoc tai len",
                        actor.getFullName() + " da tai len tep: " + file.getOriginalName(),
                        NotificationType.FILE_UPLOADED,
                        file.getId()
                ));
    }

    @Getter
    @RequiredArgsConstructor
    public static class DownloadFile {
        private final FileEntity file;
        private final Resource resource;
    }
}
