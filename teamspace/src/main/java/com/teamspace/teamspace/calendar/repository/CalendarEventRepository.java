package com.teamspace.teamspace.calendar.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamspace.teamspace.calendar.entity.CalendarEvent;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

    List<CalendarEvent> findByProjectIdOrderByStartTimeAsc(Long projectId);
}
