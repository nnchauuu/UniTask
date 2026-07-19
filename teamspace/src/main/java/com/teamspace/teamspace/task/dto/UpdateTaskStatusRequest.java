package com.teamspace.teamspace.task.dto;

import com.teamspace.teamspace.task.enums.TaskStatus;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateTaskStatusRequest {

    @NotNull(message = "Status khong duoc rong")
    private TaskStatus status;
}
