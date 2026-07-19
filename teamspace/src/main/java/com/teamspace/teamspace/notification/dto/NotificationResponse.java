package com.teamspace.teamspace.notification.dto;

import java.time.LocalDateTime;

import com.teamspace.teamspace.notification.entity.Notification;
import com.teamspace.teamspace.notification.enums.NotificationType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class NotificationResponse {

    private Long id;
    private Long receiverId;
    private String title;
    private String content;
    private NotificationType type;
    private Long relatedId;
    private Long projectId;
    private Long taskId;
    private boolean isRead;
    private LocalDateTime createdAt;

    public static NotificationResponse from(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .receiverId(notification.getReceiver().getId())
                .title(notification.getTitle())
                .content(notification.getContent())
                .type(notification.getType())
                .relatedId(notification.getRelatedId())
                .projectId(notification.getProjectId())
                .taskId(notification.getTaskId())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
