package com.teamspace.teamspace.notification.service;

import java.util.List;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamspace.teamspace.exception.ForbiddenException;
import com.teamspace.teamspace.exception.ResourceNotFoundException;
import com.teamspace.teamspace.exception.UnauthorizedException;
import com.teamspace.teamspace.notification.dto.NotificationResponse;
import com.teamspace.teamspace.notification.entity.Notification;
import com.teamspace.teamspace.notification.enums.NotificationType;
import com.teamspace.teamspace.notification.repository.NotificationRepository;
import com.teamspace.teamspace.user.entity.User;
import com.teamspace.teamspace.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(Authentication authentication) {
        User currentUser = getCurrentUser(authentication);

        return notificationRepository.findByReceiverIdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional
    public NotificationResponse markAsRead(Long notificationId, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay thong bao"));

        if (!notification.getReceiver().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Ban khong co quyen cap nhat thong bao nay");
        }

        notification.setRead(true);
        return NotificationResponse.from(notification);
    }

    @Transactional
    public void markAllAsRead(Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        notificationRepository.findByReceiverIdAndIsReadFalse(currentUser.getId())
                .forEach(notification -> notification.setRead(true));
    }

    @Transactional
    public NotificationResponse createAndSend(
            User receiver,
            String title,
            String content,
            NotificationType type,
            Long relatedId
    ) {
        return createAndSendDetailed(receiver,title,content,type,relatedId,null,relatedId,null);
    }

    @Transactional
    public NotificationResponse createAndSendDetailed(User receiver,String title,String content,NotificationType type,Long relatedId,Long projectId,Long taskId,String dedupKey) {
        if (dedupKey != null) {
            var existing = notificationRepository.findByDedupKey(dedupKey);
            if (existing.isPresent()) return NotificationResponse.from(existing.get());
        }
        Notification notification = Notification.builder()
                .receiver(receiver)
                .title(title)
                .content(content)
                .type(type)
                .relatedId(relatedId)
                .projectId(projectId)
                .taskId(taskId)
                .dedupKey(dedupKey)
                .isRead(false)
                .build();

        NotificationResponse response = NotificationResponse.from(notificationRepository.save(notification));
        messagingTemplate.convertAndSend("/topic/users/" + receiver.getId() + "/notifications", response);
        return response;
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new UnauthorizedException("Ban chua dang nhap");
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UnauthorizedException("Khong tim thay user hien tai"));
    }
}
