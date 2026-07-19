package com.teamspace.teamspace.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoogleLoginRequest {

    @NotBlank(message = "Google authorization code khong duoc de trong")
    private String code;

    @NotBlank(message = "Google redirect URI khong duoc de trong")
    private String redirectUri;
}
