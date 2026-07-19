package com.teamspace.teamspace.meeting.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamspace.teamspace.meeting.entity.Meeting;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    List<Meeting> findByProjectIdOrderByStartTimeDesc(Long projectId);
}
