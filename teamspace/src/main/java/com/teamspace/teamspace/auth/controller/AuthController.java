package com.teamspace.teamspace.auth.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.teamspace.teamspace.auth.dto.AuthResponse;
import com.teamspace.teamspace.auth.dto.LoginRequest;
import com.teamspace.teamspace.auth.dto.GoogleLoginRequest;
import com.teamspace.teamspace.auth.dto.RegisterRequest;
import com.teamspace.teamspace.auth.dto.UserResponse;
import com.teamspace.teamspace.auth.service.AuthService;
import com.teamspace.teamspace.common.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success("Đăng ký thành công", authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success("Đăng nhập thành công", authService.login(request));
    }

    @PostMapping("/google")
    public ApiResponse<AuthResponse> googleLogin(
            @RequestHeader(value = "X-Requested-With", required = false) String requestedWith,
            @Valid @RequestBody GoogleLoginRequest request) {
        return ApiResponse.success(
                "Đăng nhập Google thành công",
                authService.loginWithGoogle(request.getCode(), request.getRedirectUri(), requestedWith));
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> me(Authentication authentication) {
        return ApiResponse.success("Người dùng hiện tại", authService.getCurrentUser(authentication));
    }
}
