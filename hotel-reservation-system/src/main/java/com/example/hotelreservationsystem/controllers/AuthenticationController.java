package com.example.hotelreservationsystem.controllers;

import com.example.hotelreservationsystem.dto.LoginRequest;
import com.example.hotelreservationsystem.dto.LoginResponse;
import com.example.hotelreservationsystem.dto.RegisterRequest;
import com.example.hotelreservationsystem.dto.UserResponse;
import com.example.hotelreservationsystem.service.AuthenticationService;
import com.example.hotelreservationsystem.service.TokenStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final TokenStorageService tokenStorageService;

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@RequestBody @Validated RegisterRequest request) {
        var response = authenticationService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Validated LoginRequest request) {
        var response = authenticationService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var email = authentication.getName();
        var customer = authenticationService.getCurrentUser(email);

        var response = UserResponse.builder()
                                   .id(customer.getId())
                                   .name(customer.getName())
                                   .email(customer.getEmail())
                                   .phoneNumber(customer.getPhoneNumber())
                                   .membershipTier(customer.getMembershipTier())
                                   .role(customer.getRole())
                                   .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        var response = new HashMap<String, String>();

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            var token = authHeader.substring(7);
            var removed = tokenStorageService.removeToken(token);

            if (removed) {
                response.put("message", "Logout successful");
            } else {
                response.put("message", "Token not found or already logged out");
            }
            return ResponseEntity.ok(response);
        }

        response.put("message", "Invalid Authorization header");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        var response = new HashMap<String, String>();
        response.put("status", "Authentication service is running");
        return ResponseEntity.ok(response);
    }
}
