package com.teamspace.teamspace.task.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReorderChecklistRequest {
    @NotEmpty(message = "Danh sach checklist khong duoc rong")
    private List<Long> itemIds;
}
