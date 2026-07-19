package com.teamspace.teamspace.meetingroom.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teamspace.teamspace.common.ApiResponse;
import com.teamspace.teamspace.meetingroom.dto.CreateMeetingRoomRequest;
import com.teamspace.teamspace.meetingroom.dto.MeetingRoomResponse;
import com.teamspace.teamspace.meetingroom.dto.SignalMessageRequest;
import com.teamspace.teamspace.meetingroom.dto.SignalMessageResponse;
import com.teamspace.teamspace.meetingroom.service.MeetingRoomService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MeetingRoomController {

    private final MeetingRoomService meetingRoomService;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/projects/{projectId}/meeting-rooms")
    public ApiResponse<MeetingRoomResponse> createRoom(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateMeetingRoomRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(
                "Meeting room created successfully",
                meetingRoomService.createRoom(projectId, request, authentication)
        );
    }

    @GetMapping("/projects/{projectId}/meeting-rooms")
    public ApiResponse<List<MeetingRoomResponse>> getProjectRooms(
            @PathVariable Long projectId,
            Authentication authentication
    ) {
        return ApiResponse.success(
                "Meeting rooms fetched successfully",
                meetingRoomService.getProjectRooms(projectId, authentication)
        );
    }

    @GetMapping("/meeting-rooms/{roomId}")
    public ApiResponse<MeetingRoomResponse> getRoom(
            @PathVariable Long roomId,
            Authentication authentication
    ) {
        return ApiResponse.success(
                "Meeting room fetched successfully",
                meetingRoomService.getRoom(roomId, authentication)
        );
    }

    @MessageMapping("/meeting-rooms/{roomId}/signal")
    public void signal(
            @DestinationVariable Long roomId,
            @Payload SignalMessageRequest request,
            Principal principal
    ) {
        SignalMessageResponse response = meetingRoomService.buildSignal(roomId, request, principal);
        messagingTemplate.convertAndSend("/topic/meeting-rooms/" + roomId + "/signal", response);
    }
}
