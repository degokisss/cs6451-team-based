package com.example.hotelreservationsystem.controllers;

import com.example.hotelreservationsystem.dto.LoginRequest;
import com.example.hotelreservationsystem.dto.LoginResponse;
import com.example.hotelreservationsystem.dto.RegisterRequest;
import com.example.hotelreservationsystem.dto.UserResponse;
import com.example.hotelreservationsystem.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@RequestBody RegisterRequest request) {
        var response = authenticationService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
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

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        var response = new HashMap<String, String>();
        response.put("status", "Authentication service is running");
        return ResponseEntity.ok(response);
    }
}
