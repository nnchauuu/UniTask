package com.teamspace.teamspace.meetingroom.dto;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignalMessageRequest {

    private String type;
    private String clientId;
    private String sdp;
    private Map<String, Object> candidate;
}
