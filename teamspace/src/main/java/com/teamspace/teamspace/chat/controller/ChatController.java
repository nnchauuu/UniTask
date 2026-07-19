package com.teamspace.teamspace.chat.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teamspace.teamspace.chat.dto.ChatMessageResponse;
import com.teamspace.teamspace.chat.dto.SendChatMessageRequest;
import com.teamspace.teamspace.chat.service.ChatService;
import com.teamspace.teamspace.common.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping("/projects/{projectId}/messages")
    public ApiResponse<List<ChatMessageResponse>> getProjectMessages(
            @PathVariable Long projectId,
            Authentication authentication
    ) {
        return ApiResponse.success(
                "Lấy tin nhắn thành công",
                chatService.getProjectMessages(projectId, authentication)
        );
    }

    @MessageMapping("/projects/{projectId}/chat")
    public void sendMessage(
            @DestinationVariable Long projectId,
            @Payload SendChatMessageRequest request,
            Principal principal
    ) {
        ChatMessageResponse response = chatService.saveMessage(projectId, request, principal);
        messagingTemplate.convertAndSend("/topic/projects/" + projectId + "/chat", response);
    }
}
