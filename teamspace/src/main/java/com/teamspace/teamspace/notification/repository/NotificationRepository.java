package com.teamspace.teamspace.notification.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamspace.teamspace.notification.entity.Notification;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByReceiverIdOrderByCreatedAtDesc(Long receiverId);

    long countByReceiverIdAndIsReadFalse(Long receiverId);

    List<Notification> findByReceiverIdAndIsReadFalse(Long receiverId);
    Optional<Notification> findByDedupKey(String dedupKey);
}
