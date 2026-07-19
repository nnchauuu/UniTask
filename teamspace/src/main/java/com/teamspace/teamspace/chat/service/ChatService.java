package com.teamspace.teamspace.chat.service;

import java.security.Principal;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamspace.teamspace.chat.dto.ChatMessageResponse;
import com.teamspace.teamspace.chat.dto.SendChatMessageRequest;
import com.teamspace.teamspace.chat.entity.ChatMessage;
import com.teamspace.teamspace.chat.enums.MessageType;
import com.teamspace.teamspace.chat.repository.ChatMessageRepository;
import com.teamspace.teamspace.exception.BadRequestException;
import com.teamspace.teamspace.exception.ForbiddenException;
import com.teamspace.teamspace.exception.ResourceNotFoundException;
import com.teamspace.teamspace.exception.UnauthorizedException;
import com.teamspace.teamspace.project.entity.Project;
import com.teamspace.teamspace.project.repository.ProjectRepository;
import com.teamspace.teamspace.user.entity.User;
import com.teamspace.teamspace.user.repository.UserRepository;
import com.teamspace.teamspace.workspace.repository.WorkspaceMemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final ProjectRepository projectRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getProjectMessages(Long projectId, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        Project project = getProjectOrThrow(projectId);
        requireWorkspaceMember(project, currentUser);

        return chatMessageRepository.findByProjectIdOrderByCreatedAtAsc(projectId)
                .stream()
                .map(ChatMessageResponse::from)
                .toList();
    }

    @Transactional
    public ChatMessageResponse saveMessage(
            Long projectId,
            SendChatMessageRequest request,
            Principal principal
    ) {
        if (principal == null || principal.getName() == null) {
            throw new UnauthorizedException("Ban chua dang nhap");
        }

        User currentUser = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new UnauthorizedException("Khong tim thay user hien tai"));
        Project project = getProjectOrThrow(projectId);
        requireWorkspaceMember(project, currentUser);

        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new BadRequestException("Noi dung khong duoc rong");
        }

        ChatMessage message = ChatMessage.builder()
                .project(project)
                .sender(currentUser)
                .content(request.getContent().trim())
                .messageType(request.getMessageType() == null ? MessageType.TEXT : request.getMessageType())
                .build();

        return ChatMessageResponse.from(chatMessageRepository.save(message));
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

    private void requireWorkspaceMember(Project project, User user) {
        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(project.getWorkspace().getId(), user.getId())) {
            throw new ForbiddenException("Ban khong co quyen truy cap workspace nay");
        }
    }
}
