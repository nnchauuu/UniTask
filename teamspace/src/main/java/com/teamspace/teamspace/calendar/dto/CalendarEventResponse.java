package com.teamspace.teamspace.calendar.dto;

import java.time.LocalDateTime;

import com.teamspace.teamspace.calendar.entity.CalendarEvent;
import com.teamspace.teamspace.calendar.enums.EventType;
import com.teamspace.teamspace.meeting.entity.Meeting;
import com.teamspace.teamspace.workspace.dto.WorkspaceUserSummary;
import com.teamspace.teamspace.workspace.enums.WorkspaceRole;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class CalendarEventResponse {

    private Long id;
    private String virtualId;
    private Long projectId;
    private String projectName;
    private String title;
    private String description;
    private EventType eventType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private WorkspaceUserSummary createdBy;
    private WorkspaceUserSummary assignedTo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private WorkspaceRole myRole;
    private boolean editable;

    public static CalendarEventResponse from(CalendarEvent event, WorkspaceRole myRole) {
        return CalendarEventResponse.builder()
                .id(event.getId())
                .virtualId("MEETING-" + event.getId())
                .projectId(event.getProject().getId())
                .projectName(event.getProject().getName())
                .title(event.getTitle())
                .description(event.getDescription())
                .eventType(event.getEventType())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .createdBy(WorkspaceUserSummary.from(event.getCreatedBy()))
                .assignedTo(null)
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .myRole(myRole)
                .editable(true)
                .build();
    }

    public static CalendarEventResponse fromMeeting(Meeting meeting, WorkspaceRole myRole) {
        return CalendarEventResponse.builder()
                .id(null)
                .virtualId("PROJECT_MEETING-" + meeting.getId())
                .projectId(meeting.getProject().getId())
                .projectName(meeting.getProject().getName())
                .title(meeting.getTitle())
                .description(meeting.getDescription())
                .eventType(EventType.MEETING)
                .startTime(meeting.getStartTime())
                .endTime(meeting.getEndTime())
                .createdBy(WorkspaceUserSummary.from(meeting.getCreatedBy()))
                .assignedTo(null)
                .createdAt(meeting.getCreatedAt())
                .updatedAt(meeting.getCreatedAt())
                .myRole(myRole)
                .editable(false)
                .build();
    }
}
