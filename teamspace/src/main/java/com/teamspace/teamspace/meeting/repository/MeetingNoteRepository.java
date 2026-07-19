package com.teamspace.teamspace.meeting.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamspace.teamspace.meeting.entity.MeetingNote;

public interface MeetingNoteRepository extends JpaRepository<MeetingNote, Long> {

    Optional<MeetingNote> findByMeetingId(Long meetingId);
}
