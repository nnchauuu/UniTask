package com.teamspace.teamspace.calendar.dto;

import java.time.LocalDateTime;

import com.teamspace.teamspace.calendar.enums.EventType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCalendarEventRequest {

    @NotBlank(message = "Title khong duoc rong")
    @Size(max = 200, message = "Title toi da 200 ky tu")
    private String title;

    @Size(max = 1000, message = "Description toi da 1000 ky tu")
    private String description;

    private EventType eventType = EventType.MEETING;

    @NotNull(message = "Start time khong duoc rong")
    private LocalDateTime startTime;

    @NotNull(message = "Thoi gian ket thuc khong duoc rong")
    private LocalDateTime endTime;
}
