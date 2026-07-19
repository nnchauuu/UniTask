package com.teamspace.teamspace.meetingroom.dto;

import java.time.LocalDateTime;
import java.util.Map;

import com.teamspace.teamspace.user.entity.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class SignalMessageResponse {

    private Long roomId;
    private String type;
    private String clientId;
    private Long senderId;
    private String senderName;
    private String sdp;
    private Map<String, Object> candidate;
    private LocalDateTime sentAt;

    public static SignalMessageResponse from(Long roomId, SignalMessageRequest request, User sender) {
        return SignalMessageResponse.builder()
                .roomId(roomId)
                .type(request.getType())
                .clientId(request.getClientId())
                .senderId(sender.getId())
                .senderName(sender.getFullName())
                .sdp(request.getSdp())
                .candidate(request.getCandidate())
                .sentAt(LocalDateTime.now())
                .build();
    }
}
