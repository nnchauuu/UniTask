package com.teamspace.teamspace.meeting.dto;

import java.time.LocalDateTime;

import com.teamspace.teamspace.meeting.entity.MeetingParticipant;
import com.teamspace.teamspace.workspace.dto.WorkspaceUserSummary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class MeetingParticipantResponse {

    private Long id;
    private WorkspaceUserSummary user;
    private LocalDateTime joinedAt;

    public static MeetingParticipantResponse from(MeetingParticipant participant) {
        return MeetingParticipantResponse.builder()
                .id(participant.getId())
                .user(WorkspaceUserSummary.from(participant.getUser()))
                .joinedAt(participant.getJoinedAt())
                .build();
    }
}
