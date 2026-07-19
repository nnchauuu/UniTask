package com.teamspace.teamspace.notification.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teamspace.teamspace.common.ApiResponse;
import com.teamspace.teamspace.notification.dto.NotificationResponse;
import com.teamspace.teamspace.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<List<NotificationResponse>> getMyNotifications(Authentication authentication) {
        return ApiResponse.success(
                "Lấy thông báo thành công",
                notificationService.getMyNotifications(authentication)
        );
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<NotificationResponse> markAsRead(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ApiResponse.success(
                "Đã đánh dấu thông báo là đã đọc",
                notificationService.markAsRead(id, authentication)
        );
    }

    @PatchMapping("/read-all")
    public ApiResponse<Void> markAllAsRead(Authentication authentication) {
        notificationService.markAllAsRead(authentication);
        return ApiResponse.success("Đã đánh dấu tất cả thông báo là đã đọc");
    }
}
