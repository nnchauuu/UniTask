package com.teamspace.teamspace.file.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.teamspace.teamspace.common.ApiResponse;
import com.teamspace.teamspace.file.dto.FileResponse;
import com.teamspace.teamspace.file.service.FileService;
import com.teamspace.teamspace.file.service.FileService.DownloadFile;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping(value = "/projects/{projectId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<FileResponse> uploadProjectFile(
            @PathVariable Long projectId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {
        return ApiResponse.success(
                "Tải tệp lên thành công",
                fileService.uploadProjectFile(projectId, file, authentication)
        );
    }

    @PostMapping(value = "/tasks/{taskId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<FileResponse> uploadTaskFile(
            @PathVariable Long taskId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {
        return ApiResponse.success(
                "Tải tệp lên thành công",
                fileService.uploadTaskFile(taskId, file, authentication)
        );
    }

    @GetMapping("/projects/{projectId}/files")
    public ApiResponse<List<FileResponse>> getProjectFiles(
            @PathVariable Long projectId,
            Authentication authentication
    ) {
        return ApiResponse.success(
                "Lấy danh sách tệp thành công",
                fileService.getProjectFiles(projectId, authentication)
        );
    }

    @GetMapping("/tasks/{taskId}/files")
    public ApiResponse<List<FileResponse>> getTaskFiles(
            @PathVariable Long taskId,
            Authentication authentication
    ) {
        return ApiResponse.success(
                "Lấy danh sách tệp thành công",
                fileService.getTaskFiles(taskId, authentication)
        );
    }

    @GetMapping("/files/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable Long fileId,
            Authentication authentication
    ) {
        DownloadFile downloadFile = fileService.getDownloadFile(fileId, authentication);
        String fileType = downloadFile.getFile().getFileType();
        MediaType mediaType = fileType == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(fileType);

        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(downloadFile.getFile().getOriginalName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .contentLength(downloadFile.getFile().getFileSize())
                .body(downloadFile.getResource());
    }

    @DeleteMapping("/files/{fileId}")
    public ApiResponse<Void> deleteFile(
            @PathVariable Long fileId,
            Authentication authentication
    ) {
        fileService.deleteFile(fileId, authentication);
        return ApiResponse.success("Xóa tệp thành công");
    }
}
