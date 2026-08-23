package com.baraseck.supportflow.controller;

import com.baraseck.supportflow.dto.LoginRequest;
import com.baraseck.supportflow.dto.LoginResponse;
import com.baraseck.supportflow.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/auth") @RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    @PostMapping("/login") public LoginResponse login(@Valid @RequestBody LoginRequest request) { return authService.login(request); }
}
