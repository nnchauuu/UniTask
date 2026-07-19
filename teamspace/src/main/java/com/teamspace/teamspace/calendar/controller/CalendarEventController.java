package com.teamspace.teamspace.calendar.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teamspace.teamspace.calendar.dto.CalendarEventResponse;
import com.teamspace.teamspace.calendar.dto.CreateCalendarEventRequest;
import com.teamspace.teamspace.calendar.dto.UpdateCalendarEventRequest;
import com.teamspace.teamspace.calendar.service.CalendarEventService;
import com.teamspace.teamspace.common.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CalendarEventController {

    private final CalendarEventService calendarEventService;

    @GetMapping("/projects/{projectId}/calendar-events")
    public ApiResponse<List<CalendarEventResponse>> getProjectCalendarEvents(
            @PathVariable Long projectId,
            Authentication authentication
    ) {
        return ApiResponse.success(
                "Lấy sự kiện lịch thành công",
                calendarEventService.getProjectCalendarEvents(projectId, authentication)
        );
    }

    @GetMapping("/workspaces/{workspaceId}/calendar-events")
    public ApiResponse<List<CalendarEventResponse>> getWorkspaceCalendarEvents(
            @PathVariable Long workspaceId,
            Authentication authentication
    ) {
        return ApiResponse.success(
                "Lay lich workspace thanh cong",
                calendarEventService.getWorkspaceCalendarEvents(workspaceId, authentication)
        );
    }

    @PostMapping("/projects/{projectId}/calendar-events")
    public ApiResponse<CalendarEventResponse> createCalendarEvent(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateCalendarEventRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(
                "Tạo sự kiện lịch thành công",
                calendarEventService.createCalendarEvent(projectId, request, authentication)
        );
    }

    @PutMapping("/calendar-events/{eventId}")
    public ApiResponse<CalendarEventResponse> updateCalendarEvent(
            @PathVariable Long eventId,
            @Valid @RequestBody UpdateCalendarEventRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(
                "Cập nhật sự kiện lịch thành công",
                calendarEventService.updateCalendarEvent(eventId, request, authentication)
        );
    }

    @DeleteMapping("/calendar-events/{eventId}")
    public ApiResponse<Void> deleteCalendarEvent(
            @PathVariable Long eventId,
            Authentication authentication
    ) {
        calendarEventService.deleteCalendarEvent(eventId, authentication);
        return ApiResponse.success("Xóa sự kiện lịch thành công");
    }
}
