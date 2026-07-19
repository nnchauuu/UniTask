package com.teamspace.teamspace.file.dto;

import java.time.LocalDateTime;

import com.teamspace.teamspace.file.entity.FileEntity;
import com.teamspace.teamspace.workspace.dto.WorkspaceUserSummary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class FileResponse {

    private Long id;
    private Long projectId;
    private Long taskId;
    private WorkspaceUserSummary uploadedBy;
    private String originalName;
    private String storedName;
    private String fileType;
    private Long fileSize;
    private LocalDateTime createdAt;

    public static FileResponse from(FileEntity file) {
        return FileResponse.builder()
                .id(file.getId())
                .projectId(file.getProject().getId())
                .taskId(file.getTask() == null ? null : file.getTask().getId())
                .uploadedBy(WorkspaceUserSummary.from(file.getUploadedBy()))
                .originalName(file.getOriginalName())
                .storedName(file.getStoredName())
                .fileType(file.getFileType())
                .fileSize(file.getFileSize())
                .createdAt(file.getCreatedAt())
                .build();
    }
}
