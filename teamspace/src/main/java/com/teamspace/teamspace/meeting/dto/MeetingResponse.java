package com.teamspace.teamspace.meeting.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.teamspace.teamspace.meeting.entity.Meeting;
import com.teamspace.teamspace.meeting.entity.MeetingNote;
import com.teamspace.teamspace.meeting.entity.MeetingParticipant;
import com.teamspace.teamspace.workspace.dto.WorkspaceUserSummary;
import com.teamspace.teamspace.workspace.enums.WorkspaceRole;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class MeetingResponse {

    private Long id;
    private Long projectId;
    private String projectName;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private WorkspaceUserSummary createdBy;
    private LocalDateTime createdAt;
    private MeetingNoteResponse note;
    private List<MeetingParticipantResponse> participants;
    private WorkspaceRole myRole;
    private boolean editable;

    public static MeetingResponse from(
            Meeting meeting,
            MeetingNote note,
            List<MeetingParticipant> participants,
            WorkspaceRole myRole
    ) {
        return MeetingResponse.builder()
                .id(meeting.getId())
                .projectId(meeting.getProject().getId())
                .projectName(meeting.getProject().getName())
                .title(meeting.getTitle())
                .description(meeting.getDescription())
                .startTime(meeting.getStartTime())
                .endTime(meeting.getEndTime())
                .createdBy(WorkspaceUserSummary.from(meeting.getCreatedBy()))
                .createdAt(meeting.getCreatedAt())
                .note(MeetingNoteResponse.from(note))
                .participants(participants.stream().map(MeetingParticipantResponse::from).toList())
                .myRole(myRole)
                .editable(myRole == WorkspaceRole.OWNER || myRole == WorkspaceRole.LEADER)
                .build();
    }
}
