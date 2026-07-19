package com.teamspace.teamspace.meeting.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamspace.teamspace.meeting.entity.MeetingParticipant;

public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, Long> {

    List<MeetingParticipant> findByMeetingIdOrderByJoinedAtAsc(Long meetingId);

    Optional<MeetingParticipant> findByMeetingIdAndUserId(Long meetingId, Long userId);

    long countByMeetingProjectIdAndUserId(Long projectId, Long userId);
}
