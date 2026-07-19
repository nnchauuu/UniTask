package com.teamspace.teamspace.meetingroom.dto;

import java.time.LocalDateTime;

import com.teamspace.teamspace.meetingroom.entity.MeetingRoom;
import com.teamspace.teamspace.workspace.dto.WorkspaceUserSummary;
import com.teamspace.teamspace.workspace.enums.WorkspaceRole;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class MeetingRoomResponse {

    private Long id;
    private Long projectId;
    private String projectName;
    private String name;
    private WorkspaceUserSummary createdBy;
    private LocalDateTime createdAt;
    private WorkspaceRole myRole;

    public static MeetingRoomResponse from(MeetingRoom room, WorkspaceRole myRole) {
        return MeetingRoomResponse.builder()
                .id(room.getId())
                .projectId(room.getProject().getId())
                .projectName(room.getProject().getName())
                .name(room.getName())
                .createdBy(WorkspaceUserSummary.from(room.getCreatedBy()))
                .createdAt(room.getCreatedAt())
                .myRole(myRole)
                .build();
    }
}
