package com.teamspace.teamspace.task.dto;

import java.time.LocalDate;
import java.math.BigDecimal;

import com.teamspace.teamspace.task.enums.TaskPriority;
import com.teamspace.teamspace.task.enums.TaskStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTaskRequest {

    @NotBlank(message = "Title khong duoc rong")
    @Size(max = 200, message = "Title toi da 200 ky tu")
    private String title;

    @Size(max = 1500, message = "Description toi da 1500 ky tu")
    private String description;

    private Long assignedToUserId;

    private Long boardColumnId;

    private Long parentTaskId;

    private TaskStatus status = TaskStatus.TODO;

    private TaskPriority priority = TaskPriority.MEDIUM;

    @Size(max = 100, message = "Task type toi da 100 ky tu")
    private String type = "DESIGN";

    private Long workCategoryId;

    private LocalDate startDate;

    private LocalDate dueDate;

    @Size(max = 500, message = "Labels toi da 500 ky tu")
    private String labels;

    @DecimalMin(value = "0.0")
    private BigDecimal estimatedEffort = BigDecimal.ZERO;
    @DecimalMin(value = "0.0")
    private BigDecimal actualEffort;

    private Boolean reviewRequired = true;
}
