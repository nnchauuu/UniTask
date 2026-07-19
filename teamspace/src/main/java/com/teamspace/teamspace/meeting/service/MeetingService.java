package com.teamspace.teamspace.meeting.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamspace.teamspace.activity.enums.ActivityAction;
import com.teamspace.teamspace.activity.service.ActivityLogService;
import com.teamspace.teamspace.exception.BadRequestException;
import com.teamspace.teamspace.exception.ForbiddenException;
import com.teamspace.teamspace.exception.ResourceNotFoundException;
import com.teamspace.teamspace.exception.UnauthorizedException;
import com.teamspace.teamspace.meeting.dto.CreateMeetingRequest;
import com.teamspace.teamspace.meeting.dto.CreateMeetingTaskRequest;
import com.teamspace.teamspace.meeting.dto.MeetingNoteRequest;
import com.teamspace.teamspace.meeting.dto.MeetingResponse;
import com.teamspace.teamspace.meeting.entity.Meeting;
import com.teamspace.teamspace.meeting.entity.MeetingNote;
import com.teamspace.teamspace.meeting.entity.MeetingParticipant;
import com.teamspace.teamspace.meeting.repository.MeetingNoteRepository;
import com.teamspace.teamspace.meeting.repository.MeetingParticipantRepository;
import com.teamspace.teamspace.meeting.repository.MeetingRepository;
import com.teamspace.teamspace.notification.enums.NotificationType;
import com.teamspace.teamspace.notification.service.NotificationService;
import com.teamspace.teamspace.project.entity.Project;
import com.teamspace.teamspace.project.repository.ProjectRepository;
import com.teamspace.teamspace.task.dto.TaskResponse;
import com.teamspace.teamspace.task.service.TaskService;
import com.teamspace.teamspace.user.entity.User;
import com.teamspace.teamspace.user.repository.UserRepository;
import com.teamspace.teamspace.workspace.entity.WorkspaceMember;
import com.teamspace.teamspace.workspace.enums.WorkspaceRole;
import com.teamspace.teamspace.workspace.repository.WorkspaceMemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final MeetingNoteRepository meetingNoteRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final ProjectRepository projectRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ActivityLogService activityLogService;
    private final TaskService taskService;

    @Transactional
    public MeetingResponse createMeeting(Long projectId, CreateMeetingRequest request, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        Project project = getProjectOrThrow(projectId);
        WorkspaceMember currentMember = getCurrentMemberOrThrow(project.getWorkspace().getId(), currentUser.getId());
        requireOwnerOrLeader(currentMember);
        validateTimeRange(request.getStartTime(), request.getEndTime());

        Meeting meeting = Meeting.builder()
                .project(project)
                .title(request.getTitle().trim())
                .description(normalizeText(request.getDescription()))
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .createdBy(currentUser)
                .build();

        Meeting savedMeeting = meetingRepository.save(meeting);
        MeetingParticipant creatorParticipant = meetingParticipantRepository.save(MeetingParticipant.builder()
                .meeting(savedMeeting)
                .user(currentUser)
                .build());

        activityLogService.log(
                project,
                currentUser,
                ActivityAction.MEETING_CREATED,
                "MEETING",
                savedMeeting.getId(),
                currentUser.getFullName() + " created meeting: " + savedMeeting.getTitle()
        );
        notifyMeetingCreated(project, savedMeeting, currentUser);

        return MeetingResponse.from(savedMeeting, null, List.of(creatorParticipant), currentMember.getRole());
    }

    @Transactional(readOnly = true)
    public List<MeetingResponse> getProjectMeetings(Long projectId, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        Project project = getProjectOrThrow(projectId);
        WorkspaceMember currentMember = getCurrentMemberOrThrow(project.getWorkspace().getId(), currentUser.getId());

        return meetingRepository.findByProjectIdOrderByStartTimeDesc(projectId)
                .stream()
                .map(meeting -> buildMeetingResponse(meeting, currentMember.getRole()))
                .toList();
    }

    @Transactional(readOnly = true)
    public MeetingResponse getMeetingDetail(Long meetingId, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        Meeting meeting = getMeetingOrThrow(meetingId);
        WorkspaceMember currentMember = getCurrentMemberOrThrow(
                meeting.getProject().getWorkspace().getId(),
                currentUser.getId()
        );

        return buildMeetingResponse(meeting, currentMember.getRole());
    }

    @Transactional
    public MeetingResponse updateMeetingNotes(
            Long meetingId,
            MeetingNoteRequest request,
            Authentication authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        Meeting meeting = getMeetingOrThrow(meetingId);
        WorkspaceMember currentMember = getCurrentMemberOrThrow(
                meeting.getProject().getWorkspace().getId(),
                currentUser.getId()
        );
        requireOwnerOrLeader(currentMember);

        MeetingNote note = meetingNoteRepository.findByMeetingId(meetingId)
                .orElseGet(() -> MeetingNote.builder().meeting(meeting).build());
        note.setContent(normalizeText(request.getContent()));
        note.setDecisions(normalizeText(request.getDecisions()));
        meetingNoteRepository.save(note);

        return buildMeetingResponse(meeting, currentMember.getRole());
    }

    @Transactional
    public MeetingResponse addParticipant(Long meetingId, Long userId, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        Meeting meeting = getMeetingOrThrow(meetingId);
        WorkspaceMember currentMember = getCurrentMemberOrThrow(
                meeting.getProject().getWorkspace().getId(),
                currentUser.getId()
        );
        User participantUser = getWorkspaceUserOrThrow(meeting.getProject().getWorkspace().getId(), userId);

        meetingParticipantRepository.findByMeetingIdAndUserId(meetingId, userId)
                .orElseGet(() -> meetingParticipantRepository.save(MeetingParticipant.builder()
                        .meeting(meeting)
                        .user(participantUser)
                        .build()));

        return buildMeetingResponse(meeting, currentMember.getRole());
    }

    @Transactional
    public TaskResponse createTaskFromMeeting(
            Long meetingId,
            CreateMeetingTaskRequest request,
            Authentication authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        Meeting meeting = getMeetingOrThrow(meetingId);
        Project project = meeting.getProject();
        WorkspaceMember currentMember = getCurrentMemberOrThrow(project.getWorkspace().getId(), currentUser.getId());
        requireOwnerOrLeader(currentMember);

        return taskService.createTask(project.getId(), request, authentication);
    }

    private MeetingResponse buildMeetingResponse(Meeting meeting, WorkspaceRole myRole) {
        MeetingNote note = meetingNoteRepository.findByMeetingId(meeting.getId()).orElse(null);
        List<MeetingParticipant> participants = meetingParticipantRepository.findByMeetingIdOrderByJoinedAtAsc(meeting.getId());
        return MeetingResponse.from(meeting, note, participants, myRole);
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

    private Meeting getMeetingOrThrow(Long meetingId) {
        return meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay meeting"));
    }

    private WorkspaceMember getCurrentMemberOrThrow(Long workspaceId, Long userId) {
        return workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new ForbiddenException("Ban khong co quyen truy cap workspace nay"));
    }

    private User getWorkspaceUserOrThrow(Long workspaceId, Long userId) {
        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new ForbiddenException("User phai thuoc workspace cua meeting");
        }

        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay user"));
    }

    private void requireOwnerOrLeader(WorkspaceMember member) {
        if (member.getRole() != WorkspaceRole.OWNER && member.getRole() != WorkspaceRole.LEADER) {
            throw new ForbiddenException("Chi OWNER hoac LEADER moi duoc thuc hien hanh dong nay");
        }
    }

    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (endTime.isBefore(startTime)) {
            throw new BadRequestException("Thoi gian ket thuc phai sau hoac bang thoi gian bat dau");
        }
    }

    private String normalizeText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        return text.trim();
    }

    private void notifyMeetingCreated(Project project, Meeting meeting, User actor) {
        workspaceMemberRepository.findByWorkspaceIdOrderByJoinedAtAsc(project.getWorkspace().getId())
                .stream()
                .map(WorkspaceMember::getUser)
                .filter(user -> !user.getId().equals(actor.getId()))
                .forEach(user -> notificationService.createAndSend(
                        user,
                        "Cuoc hop moi da duoc tao",
                        actor.getFullName() + " da tao cuoc hop: " + meeting.getTitle(),
                        NotificationType.MEETING_CREATED,
                        meeting.getId()
                ));
    }

}
