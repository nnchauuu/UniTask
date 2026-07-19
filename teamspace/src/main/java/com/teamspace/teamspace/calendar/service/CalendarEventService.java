package com.teamspace.teamspace.calendar.service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamspace.teamspace.activity.enums.ActivityAction;
import com.teamspace.teamspace.activity.service.ActivityLogService;
import com.teamspace.teamspace.calendar.dto.CalendarEventResponse;
import com.teamspace.teamspace.calendar.dto.CreateCalendarEventRequest;
import com.teamspace.teamspace.calendar.dto.UpdateCalendarEventRequest;
import com.teamspace.teamspace.calendar.entity.CalendarEvent;
import com.teamspace.teamspace.calendar.enums.EventType;
import com.teamspace.teamspace.calendar.repository.CalendarEventRepository;
import com.teamspace.teamspace.exception.BadRequestException;
import com.teamspace.teamspace.exception.ForbiddenException;
import com.teamspace.teamspace.exception.ResourceNotFoundException;
import com.teamspace.teamspace.exception.UnauthorizedException;
import com.teamspace.teamspace.meeting.repository.MeetingRepository;
import com.teamspace.teamspace.notification.enums.NotificationType;
import com.teamspace.teamspace.notification.service.NotificationService;
import com.teamspace.teamspace.project.entity.Project;
import com.teamspace.teamspace.project.repository.ProjectRepository;
import com.teamspace.teamspace.task.entity.Task;
import com.teamspace.teamspace.task.repository.TaskRepository;
import com.teamspace.teamspace.user.entity.User;
import com.teamspace.teamspace.user.repository.UserRepository;
import com.teamspace.teamspace.workspace.dto.WorkspaceUserSummary;
import com.teamspace.teamspace.workspace.entity.WorkspaceMember;
import com.teamspace.teamspace.workspace.enums.WorkspaceRole;
import com.teamspace.teamspace.workspace.repository.WorkspaceMemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CalendarEventService {

    private final CalendarEventRepository calendarEventRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final MeetingRepository meetingRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ActivityLogService activityLogService;

    @Transactional(readOnly = true)
    public List<CalendarEventResponse> getProjectCalendarEvents(Long projectId, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        Project project = getProjectOrThrow(projectId);
        WorkspaceMember currentMember = getCurrentMemberOrThrow(project.getWorkspace().getId(), currentUser.getId());

        List<CalendarEventResponse> storedEvents = calendarEventRepository.findByProjectIdOrderByStartTimeAsc(projectId)
                .stream()
                .map(event -> CalendarEventResponse.from(event, currentMember.getRole()))
                .toList();

        List<CalendarEventResponse> taskDeadlineEvents = taskRepository.findByProjectIdOrderByCreatedAtDesc(projectId)
                .stream()
                .filter(task -> task.getDueDate() != null)
                .map(task -> buildTaskDeadlineEvent(project, task, currentMember.getRole()))
                .toList();

        List<CalendarEventResponse> meetingEvents = meetingRepository.findByProjectIdOrderByStartTimeDesc(projectId)
                .stream()
                .map(meeting -> CalendarEventResponse.fromMeeting(meeting, currentMember.getRole()))
                .toList();

        List<CalendarEventResponse> projectDeadlineEvent = project.getEndDate() == null
                ? List.of()
                : List.of(buildProjectDeadlineEvent(project, currentMember.getRole()));

        return java.util.stream.Stream.of(storedEvents, taskDeadlineEvents, meetingEvents, projectDeadlineEvent)
                .flatMap(List::stream)
                .sorted(Comparator.comparing(CalendarEventResponse::getStartTime))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CalendarEventResponse> getWorkspaceCalendarEvents(Long workspaceId, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        WorkspaceMember currentMember = getCurrentMemberOrThrow(workspaceId, currentUser.getId());

        return projectRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId)
                .stream()
                .flatMap(project -> {
                    List<CalendarEventResponse> storedEvents = calendarEventRepository
                            .findByProjectIdOrderByStartTimeAsc(project.getId())
                            .stream()
                            .map(event -> CalendarEventResponse.from(event, currentMember.getRole()))
                            .toList();
                    List<CalendarEventResponse> taskEvents = taskRepository
                            .findByProjectIdOrderByCreatedAtDesc(project.getId())
                            .stream()
                            .filter(task -> task.getDueDate() != null)
                            .map(task -> buildTaskDeadlineEvent(project, task, currentMember.getRole()))
                            .toList();
                    List<CalendarEventResponse> meetingEvents = meetingRepository
                            .findByProjectIdOrderByStartTimeDesc(project.getId())
                            .stream()
                            .map(meeting -> CalendarEventResponse.fromMeeting(meeting, currentMember.getRole()))
                            .toList();
                    List<CalendarEventResponse> projectEvents = project.getEndDate() == null
                            ? List.of()
                            : List.of(buildProjectDeadlineEvent(project, currentMember.getRole()));
                    return java.util.stream.Stream.of(storedEvents, taskEvents, meetingEvents, projectEvents).flatMap(List::stream);
                })
                .sorted(Comparator.comparing(CalendarEventResponse::getStartTime))
                .toList();
    }

    @Transactional
    public CalendarEventResponse createCalendarEvent(
            Long projectId,
            CreateCalendarEventRequest request,
            Authentication authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        Project project = getProjectOrThrow(projectId);
        WorkspaceMember currentMember = getCurrentMemberOrThrow(project.getWorkspace().getId(), currentUser.getId());
        requireOwnerOrLeader(currentMember);
        ensureEditableEvent(request.getEventType());
        validateTimeRange(request.getStartTime(), request.getEndTime());
        EventType eventType = request.getEventType() == null ? EventType.CUSTOM_EVENT : request.getEventType();

        CalendarEvent event = CalendarEvent.builder()
                .project(project)
                .title(request.getTitle().trim())
                .description(normalizeText(request.getDescription()))
                .eventType(eventType)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .createdBy(currentUser)
                .build();

        CalendarEvent savedEvent = calendarEventRepository.save(event);
        activityLogService.log(
                project,
                currentUser,
                ActivityAction.MEETING_CREATED,
                "MEETING",
                savedEvent.getId(),
                currentUser.getFullName() + " created meeting: " + savedEvent.getTitle()
        );
        notifyMeetingCreated(project, savedEvent, currentUser);
        return CalendarEventResponse.from(savedEvent, currentMember.getRole());
    }

    @Transactional
    public CalendarEventResponse updateCalendarEvent(
            Long eventId,
            UpdateCalendarEventRequest request,
            Authentication authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        CalendarEvent event = getCalendarEventOrThrow(eventId);
        WorkspaceMember currentMember = getCurrentMemberOrThrow(event.getProject().getWorkspace().getId(), currentUser.getId());
        requireOwnerOrLeader(currentMember);
        ensureEditableEvent(event.getEventType());
        ensureEditableEvent(request.getEventType());
        validateTimeRange(request.getStartTime(), request.getEndTime());

        event.setTitle(request.getTitle().trim());
        event.setDescription(normalizeText(request.getDescription()));
        event.setEventType(request.getEventType() == null ? event.getEventType() : request.getEventType());
        event.setStartTime(request.getStartTime());
        event.setEndTime(request.getEndTime());

        return CalendarEventResponse.from(event, currentMember.getRole());
    }

    @Transactional
    public void deleteCalendarEvent(Long eventId, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        CalendarEvent event = getCalendarEventOrThrow(eventId);
        WorkspaceMember currentMember = getCurrentMemberOrThrow(event.getProject().getWorkspace().getId(), currentUser.getId());
        requireOwnerOrLeader(currentMember);
        ensureEditableEvent(event.getEventType());

        calendarEventRepository.delete(event);
    }

    private CalendarEventResponse buildTaskDeadlineEvent(Project project, Task task, WorkspaceRole myRole) {
        LocalDateTime deadline = task.getDueDate().atTime(LocalTime.of(23, 59));
        return CalendarEventResponse.builder()
                .id(null)
                .virtualId("TASK_DEADLINE-" + task.getId())
                .projectId(project.getId())
                .projectName(project.getName())
                .title(task.getTitle())
                .description(task.getDescription())
                .eventType(EventType.TASK_DEADLINE)
                .startTime(deadline)
                .endTime(deadline)
                .createdBy(null)
                .assignedTo(task.getAssignedTo() == null ? null : WorkspaceUserSummary.from(task.getAssignedTo()))
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .myRole(myRole)
                .editable(false)
                .build();
    }

    private CalendarEventResponse buildProjectDeadlineEvent(Project project, WorkspaceRole myRole) {
        LocalDateTime deadline = project.getEndDate().atTime(LocalTime.of(23, 59));
        return CalendarEventResponse.builder()
                .id(null)
                .virtualId("PROJECT_DEADLINE-" + project.getId())
                .projectId(project.getId())
                .projectName(project.getName())
                .title(project.getName())
                .description(project.getDescription())
                .eventType(EventType.PROJECT_DEADLINE)
                .startTime(deadline)
                .endTime(deadline)
                .createdBy(null)
                .assignedTo(null)
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .myRole(myRole)
                .editable(false)
                .build();
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

    private CalendarEvent getCalendarEventOrThrow(Long eventId) {
        return calendarEventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay calendar event"));
    }

    private WorkspaceMember getCurrentMemberOrThrow(Long workspaceId, Long userId) {
        return workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new ForbiddenException("Ban khong co quyen truy cap workspace nay"));
    }

    private void requireOwnerOrLeader(WorkspaceMember member) {
        if (member.getRole() != WorkspaceRole.OWNER && member.getRole() != WorkspaceRole.LEADER) {
            throw new ForbiddenException("Chi OWNER hoac LEADER moi duoc thuc hien hanh dong nay");
        }
    }

    private void ensureEditableEvent(EventType eventType) {
        if (eventType != null && eventType != EventType.MEETING && eventType != EventType.CUSTOM_EVENT) {
            throw new BadRequestException("Chi duoc tao, sua hoac xoa cuoc hop va su kien rieng");
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

    private void notifyMeetingCreated(Project project, CalendarEvent event, User actor) {
        workspaceMemberRepository.findByWorkspaceIdOrderByJoinedAtAsc(project.getWorkspace().getId())
                .stream()
                .map(WorkspaceMember::getUser)
                .filter(user -> !user.getId().equals(actor.getId()))
                .forEach(user -> notificationService.createAndSend(
                        user,
                        "Cuoc hop moi da duoc tao",
                        actor.getFullName() + " da tao cuoc hop: " + event.getTitle(),
                        NotificationType.MEETING_CREATED,
                        event.getId()
                ));
    }
}
