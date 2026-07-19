package com.teamspace.teamspace.task.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTaskCommentRequest {

    @NotBlank(message = "Noi dung khong duoc rong")
    @Size(max = 2000, message = "Content toi da 2000 ky tu")
    private String content;
    private List<Long> mentionedUserIds = List.of();
}
