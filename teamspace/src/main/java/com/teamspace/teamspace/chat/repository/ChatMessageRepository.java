package com.teamspace.teamspace.chat.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamspace.teamspace.chat.entity.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByProjectIdOrderByCreatedAtAsc(Long projectId);
}
