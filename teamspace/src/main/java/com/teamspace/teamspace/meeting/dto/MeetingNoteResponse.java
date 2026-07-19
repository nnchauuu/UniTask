package com.teamspace.teamspace.meeting.dto;

import java.time.LocalDateTime;

import com.teamspace.teamspace.meeting.entity.MeetingNote;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class MeetingNoteResponse {

    private Long id;
    private String content;
    private String decisions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MeetingNoteResponse from(MeetingNote note) {
        if (note == null) {
            return null;
        }

        return MeetingNoteResponse.builder()
                .id(note.getId())
                .content(note.getContent())
                .decisions(note.getDecisions())
                .createdAt(note.getCreatedAt())
                .updatedAt(note.getUpdatedAt())
                .build();
    }
}
