package com.teamspace.teamspace.chat.dto;

import com.teamspace.teamspace.chat.enums.MessageType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendChatMessageRequest {

    @NotBlank(message = "Noi dung khong duoc rong")
    @Size(max = 2000, message = "Content toi da 2000 ky tu")
    private String content;

    private MessageType messageType = MessageType.TEXT;
}
