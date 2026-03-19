package com.homestay.controller;

import com.homestay.common.ApiResponse;
import com.homestay.dto.AuthDtos.HostApplyRequest;
import com.homestay.dto.AuthDtos.LoginRequest;
import com.homestay.dto.AuthDtos.RegisterRequest;
import com.homestay.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<?> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok("注册成功", authService.register(request));
    }

    @PostMapping("/host-apply")
    public ApiResponse<?> hostApply(@Valid @RequestBody HostApplyRequest request) {
        return ApiResponse.ok("申请已提交", authService.applyHost(request));
    }

    @PostMapping("/login")
    public ApiResponse<?> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok("登录成功", authService.login(request));
    }
}
