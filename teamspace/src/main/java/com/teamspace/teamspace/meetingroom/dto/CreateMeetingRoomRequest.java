package com.teamspace.teamspace.meetingroom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateMeetingRoomRequest {

    @NotBlank(message = "Room name khong duoc rong")
    @Size(max = 150, message = "Room name toi da 150 ky tu")
    private String name;
}
