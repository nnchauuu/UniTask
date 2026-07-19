package com.teamspace.teamspace.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.teamspace.teamspace.auth.service.CustomUserDetailsService;
import com.teamspace.teamspace.auth.service.JwtService;
import com.teamspace.teamspace.meetingroom.entity.MeetingRoom;
import com.teamspace.teamspace.meetingroom.repository.MeetingRoomRepository;
import com.teamspace.teamspace.project.entity.Project;
import com.teamspace.teamspace.project.repository.ProjectRepository;
import com.teamspace.teamspace.user.entity.User;
import com.teamspace.teamspace.user.repository.UserRepository;
import com.teamspace.teamspace.workspace.repository.WorkspaceMemberRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final ProjectRepository projectRepository;
    private final MeetingRoomRepository meetingRoomRepository;
    private final UserRepository userRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticateConnect(accessor);
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeProjectTopicSubscription(accessor);
            authorizeMeetingRoomSubscription(accessor);
            authorizeUserNotificationSubscription(accessor);
        }

        return message;
    }

    private void authorizeMeetingRoomSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null || !destination.matches("^/topic/meeting-rooms/\\d+/signal$")) {
            return;
        }

        if (accessor.getUser() == null || accessor.getUser().getName() == null) {
            throw new AccessDeniedException("Thieu user websocket");
        }

        Long roomId = Long.valueOf(destination.replaceAll("^/topic/meeting-rooms/(\\d+)/signal$", "$1"));
        MeetingRoom room = meetingRoomRepository.findById(roomId)
                .orElseThrow(() -> new AccessDeniedException("Khong tim thay phong hop"));
        User user = userRepository.findByEmail(accessor.getUser().getName())
                .orElseThrow(() -> new AccessDeniedException("Khong tim thay user"));

        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(room.getProject().getWorkspace().getId(), user.getId())) {
            throw new AccessDeniedException("Khong co quyen dang ky kenh phong hop");
        }
    }

    private void authenticateConnect(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new AccessDeniedException("Thieu token websocket");
        }

        String token = authHeader.substring(7);
        String email = jwtService.extractUsername(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        if (!jwtService.isTokenValid(token, userDetails)) {
            throw new AccessDeniedException("Token websocket khong hop le");
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        accessor.setUser(authentication);
    }

    private void authorizeProjectTopicSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null || !destination.matches("^/topic/projects/\\d+/(chat|board)$")) {
            return;
        }

        if (accessor.getUser() == null || accessor.getUser().getName() == null) {
            throw new AccessDeniedException("Thieu user websocket");
        }

        Long projectId = Long.valueOf(destination.replaceAll("^/topic/projects/(\\d+)/(chat|board)$", "$1"));
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new AccessDeniedException("Khong tim thay du an"));
        User user = userRepository.findByEmail(accessor.getUser().getName())
                .orElseThrow(() -> new AccessDeniedException("Khong tim thay user"));

        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(project.getWorkspace().getId(), user.getId())) {
            throw new AccessDeniedException("Khong co quyen dang ky kenh chat du an");
        }
    }

    private void authorizeUserNotificationSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null || !destination.matches("^/topic/users/\\d+/notifications$")) {
            return;
        }

        if (accessor.getUser() == null || accessor.getUser().getName() == null) {
            throw new AccessDeniedException("Thieu user websocket");
        }

        Long userId = Long.valueOf(destination.replaceAll("^/topic/users/(\\d+)/notifications$", "$1"));
        User user = userRepository.findByEmail(accessor.getUser().getName())
                .orElseThrow(() -> new AccessDeniedException("Khong tim thay user"));

        if (!user.getId().equals(userId)) {
            throw new AccessDeniedException("Khong co quyen dang ky kenh thong bao");
        }
    }
}
