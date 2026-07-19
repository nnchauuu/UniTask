package com.teamspace.teamspace.meetingroom.service;

import java.security.Principal;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamspace.teamspace.exception.ForbiddenException;
import com.teamspace.teamspace.exception.ResourceNotFoundException;
import com.teamspace.teamspace.exception.UnauthorizedException;
import com.teamspace.teamspace.meetingroom.dto.CreateMeetingRoomRequest;
import com.teamspace.teamspace.meetingroom.dto.MeetingRoomResponse;
import com.teamspace.teamspace.meetingroom.dto.SignalMessageRequest;
import com.teamspace.teamspace.meetingroom.dto.SignalMessageResponse;
import com.teamspace.teamspace.meetingroom.entity.MeetingRoom;
import com.teamspace.teamspace.meetingroom.repository.MeetingRoomRepository;
import com.teamspace.teamspace.project.entity.Project;
import com.teamspace.teamspace.project.repository.ProjectRepository;
import com.teamspace.teamspace.user.entity.User;
import com.teamspace.teamspace.user.repository.UserRepository;
import com.teamspace.teamspace.workspace.entity.WorkspaceMember;
import com.teamspace.teamspace.workspace.repository.WorkspaceMemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MeetingRoomService {

    private final MeetingRoomRepository meetingRoomRepository;
    private final ProjectRepository projectRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;

    @Transactional
    public MeetingRoomResponse createRoom(
            Long projectId,
            CreateMeetingRoomRequest request,
            Authentication authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        Project project = getProjectOrThrow(projectId);
        WorkspaceMember member = getCurrentMemberOrThrow(project.getWorkspace().getId(), currentUser.getId());

        MeetingRoom room = MeetingRoom.builder()
                .project(project)
                .name(request.getName().trim())
                .createdBy(currentUser)
                .build();

        return MeetingRoomResponse.from(meetingRoomRepository.save(room), member.getRole());
    }

    @Transactional(readOnly = true)
    public List<MeetingRoomResponse> getProjectRooms(Long projectId, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        Project project = getProjectOrThrow(projectId);
        WorkspaceMember member = getCurrentMemberOrThrow(project.getWorkspace().getId(), currentUser.getId());

        return meetingRoomRepository.findByProjectIdOrderByCreatedAtDesc(projectId)
                .stream()
                .map(room -> MeetingRoomResponse.from(room, member.getRole()))
                .toList();
    }

    @Transactional(readOnly = true)
    public MeetingRoomResponse getRoom(Long roomId, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        MeetingRoom room = getRoomOrThrow(roomId);
        WorkspaceMember member = getCurrentMemberOrThrow(room.getProject().getWorkspace().getId(), currentUser.getId());

        return MeetingRoomResponse.from(room, member.getRole());
    }

    @Transactional(readOnly = true)
    public SignalMessageResponse buildSignal(Long roomId, SignalMessageRequest request, Principal principal) {
        User currentUser = getCurrentUser(principal);
        MeetingRoom room = getRoomOrThrow(roomId);
        getCurrentMemberOrThrow(room.getProject().getWorkspace().getId(), currentUser.getId());

        return SignalMessageResponse.from(roomId, request, currentUser);
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new UnauthorizedException("Ban chua dang nhap");
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UnauthorizedException("Khong tim thay user hien tai"));
    }

    private User getCurrentUser(Principal principal) {
        if (principal == null || principal.getName() == null) {
            throw new UnauthorizedException("Ban chua dang nhap");
        }

        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new UnauthorizedException("Khong tim thay user hien tai"));
    }

    private Project getProjectOrThrow(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay project"));
    }

    private MeetingRoom getRoomOrThrow(Long roomId) {
        return meetingRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay meeting room"));
    }

    private WorkspaceMember getCurrentMemberOrThrow(Long workspaceId, Long userId) {
        return workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new ForbiddenException("Ban khong co quyen truy cap workspace nay"));
    }
}
