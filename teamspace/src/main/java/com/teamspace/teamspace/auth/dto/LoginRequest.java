package com.teamspace.teamspace.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "Email khong duoc rong")
    @Email(message = "Email khong dung dinh dang")
    private String email;

    @NotBlank(message = "Password khong duoc rong")
    private String password;
}
