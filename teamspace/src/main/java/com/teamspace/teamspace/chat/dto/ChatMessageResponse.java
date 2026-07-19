package com.teamspace.teamspace.chat.dto;

import java.time.LocalDateTime;

import com.teamspace.teamspace.chat.entity.ChatMessage;
import com.teamspace.teamspace.chat.enums.MessageType;
import com.teamspace.teamspace.workspace.dto.WorkspaceUserSummary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ChatMessageResponse {

    private Long id;
    private Long projectId;
    private WorkspaceUserSummary sender;
    private String content;
    private MessageType messageType;
    private LocalDateTime createdAt;

    public static ChatMessageResponse from(ChatMessage message) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .projectId(message.getProject().getId())
                .sender(WorkspaceUserSummary.from(message.getSender()))
                .content(message.getContent())
                .messageType(message.getMessageType())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
