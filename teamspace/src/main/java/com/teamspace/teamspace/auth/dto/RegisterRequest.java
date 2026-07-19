package com.teamspace.teamspace.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "Full name khong duoc rong")
    private String fullName;

    @NotBlank(message = "Email khong duoc rong")
    @Email(message = "Email khong dung dinh dang")
    private String email;

    @NotBlank(message = "Password khong duoc rong")
    @Size(min = 6, message = "Password phai co toi thieu 6 ky tu")
    private String password;
}
