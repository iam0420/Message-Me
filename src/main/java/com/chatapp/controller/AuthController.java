package com.chatapp.controller;

import com.chatapp.dto.request.*;
import com.chatapp.dto.response.*;
import com.chatapp.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/auth") @RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse> signup(@Valid @RequestBody SignupRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("User registered", authService.signup(req)));
    }
    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Login successful", authService.login(req)));
    }
}
