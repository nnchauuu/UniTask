package com.teamspace.teamspace.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RequestChangesReviewRequest {
    @NotBlank(message = "Ly do yeu cau chinh sua la bat buoc")
    @Size(max = 1000, message = "Ly do toi da 1000 ky tu")
    private String reason;
}
