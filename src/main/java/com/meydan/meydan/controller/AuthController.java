package com.meydan.meydan.controller;


import com.meydan.meydan.dto.Auth.AuthResponse;
import com.meydan.meydan.request.Auth.LoginRequest;
import com.meydan.meydan.request.Auth.RegisterRequest;
import com.meydan.meydan.service.AuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public String register(@RequestBody RegisterRequest req) {
        return authService.register(req);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest req) {

        return authService.login(req);
    }
}
