package com.teamspace.teamspace.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateChecklistItemRequest {
    @NotBlank(message = "Noi dung checklist khong duoc rong")
    @Size(max = 300, message = "Noi dung checklist toi da 300 ky tu")
    private String content;
    private boolean completed;
}
