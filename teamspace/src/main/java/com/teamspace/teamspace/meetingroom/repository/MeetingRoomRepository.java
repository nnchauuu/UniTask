package com.teamspace.teamspace.meetingroom.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamspace.teamspace.meetingroom.entity.MeetingRoom;

public interface MeetingRoomRepository extends JpaRepository<MeetingRoom, Long> {

    List<MeetingRoom> findByProjectIdOrderByCreatedAtDesc(Long projectId);
}
