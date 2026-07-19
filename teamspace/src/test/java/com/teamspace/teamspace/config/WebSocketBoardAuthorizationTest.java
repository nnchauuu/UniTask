package com.teamspace.teamspace.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.teamspace.teamspace.auth.service.CustomUserDetailsService;
import com.teamspace.teamspace.auth.service.JwtService;
import com.teamspace.teamspace.meetingroom.repository.MeetingRoomRepository;
import com.teamspace.teamspace.project.entity.Project;
import com.teamspace.teamspace.project.repository.ProjectRepository;
import com.teamspace.teamspace.user.entity.User;
import com.teamspace.teamspace.user.repository.UserRepository;
import com.teamspace.teamspace.workspace.entity.Workspace;
import com.teamspace.teamspace.workspace.repository.WorkspaceMemberRepository;

@ExtendWith(MockitoExtension.class)
class WebSocketBoardAuthorizationTest {
    @Mock JwtService jwtService;
    @Mock CustomUserDetailsService userDetailsService;
    @Mock ProjectRepository projectRepository;
    @Mock MeetingRoomRepository meetingRoomRepository;
    @Mock UserRepository userRepository;
    @Mock WorkspaceMemberRepository memberRepository;
    @Mock MessageChannel channel;

    @Test
    void rejectsBoardSubscriptionFromNonMember() {
        WebSocketAuthChannelInterceptor interceptor = new WebSocketAuthChannelInterceptor(jwtService,
                userDetailsService, projectRepository, meetingRoomRepository, userRepository, memberRepository);
        User user = User.builder().id(1L).email("outside@example.com").fullName("Outside").password("x").build();
        Workspace workspace = Workspace.builder().id(10L).createdBy(user).build();
        Project project = Project.builder().id(20L).workspace(workspace).createdBy(user).build();
        when(projectRepository.findById(20L)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(memberRepository.existsByWorkspaceIdAndUserId(10L, 1L)).thenReturn(false);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/projects/20/board");
        accessor.setUser(new UsernamePasswordAuthenticationToken(user.getEmail(), null));
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Khong co quyen");
    }
}
