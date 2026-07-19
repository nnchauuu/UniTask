package com.teamspace.teamspace.meeting.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teamspace.teamspace.common.ApiResponse;
import com.teamspace.teamspace.meeting.dto.CreateMeetingRequest;
import com.teamspace.teamspace.meeting.dto.CreateMeetingTaskRequest;
import com.teamspace.teamspace.meeting.dto.MeetingNoteRequest;
import com.teamspace.teamspace.meeting.dto.MeetingResponse;
import com.teamspace.teamspace.meeting.service.MeetingService;
import com.teamspace.teamspace.task.dto.TaskResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class MeetingController {

    private final MeetingService meetingService;

    @PostMapping("/projects/{projectId}/meetings")
    public ApiResponse<MeetingResponse> createMeeting(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateMeetingRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(
                "Meeting created successfully",
                meetingService.createMeeting(projectId, request, authentication)
        );
    }

    @GetMapping("/projects/{projectId}/meetings")
    public ApiResponse<List<MeetingResponse>> getProjectMeetings(
            @PathVariable Long projectId,
            Authentication authentication
    ) {
        return ApiResponse.success(
                "Meetings fetched successfully",
                meetingService.getProjectMeetings(projectId, authentication)
        );
    }

    @GetMapping("/meetings/{meetingId}")
    public ApiResponse<MeetingResponse> getMeetingDetail(
            @PathVariable Long meetingId,
            Authentication authentication
    ) {
        return ApiResponse.success(
                "Meeting fetched successfully",
                meetingService.getMeetingDetail(meetingId, authentication)
        );
    }

    @PutMapping("/meetings/{meetingId}/notes")
    public ApiResponse<MeetingResponse> updateMeetingNotes(
            @PathVariable Long meetingId,
            @RequestBody MeetingNoteRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(
                "Meeting notes updated successfully",
                meetingService.updateMeetingNotes(meetingId, request, authentication)
        );
    }

    @PostMapping("/meetings/{meetingId}/participants/{userId}")
    public ApiResponse<MeetingResponse> addParticipant(
            @PathVariable Long meetingId,
            @PathVariable Long userId,
            Authentication authentication
    ) {
        return ApiResponse.success(
                "Meeting participant added successfully",
                meetingService.addParticipant(meetingId, userId, authentication)
        );
    }

    @PostMapping("/meetings/{meetingId}/tasks")
    public ApiResponse<TaskResponse> createTaskFromMeeting(
            @PathVariable Long meetingId,
            @Valid @RequestBody CreateMeetingTaskRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(
                "Task created from meeting successfully",
                meetingService.createTaskFromMeeting(meetingId, request, authentication)
        );
    }
}
