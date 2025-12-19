package com.example.hotelreservationsystem.controllers;

import com.example.hotelreservationsystem.dto.LoginRequest;
import com.example.hotelreservationsystem.dto.LoginResponse;
import com.example.hotelreservationsystem.dto.RegisterRequest;
import com.example.hotelreservationsystem.dto.UserResponse;
import com.example.hotelreservationsystem.pattern.InterceptorChain;
import com.example.hotelreservationsystem.pattern.InterceptorManager;
import com.example.hotelreservationsystem.pattern.RequestContext;
import com.example.hotelreservationsystem.service.AuthenticationService;
import com.example.hotelreservationsystem.service.TokenStorageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthenticationController {

    private static final String MESSAGE_KEY = "message";

    private final AuthenticationService authenticationService;
    private final TokenStorageService tokenStorageService;
    private final InterceptorManager interceptorManager;

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@RequestBody @Validated RegisterRequest request) {
        var response = authenticationService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Validated LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        // Create request context
        RequestContext context = new RequestContext(httpRequest, httpResponse);

        // Get interceptor chain (configured by InterceptorManager)
        InterceptorChain chain = interceptorManager.createAuthChain();

        try {
            // Execute interceptor chain around the operation
            LoginResponse response = chain.executeChain(context, () -> authenticationService.login(request));

            return ResponseEntity.ok(response);

        } catch (Exception ex) {
            log.error("Login failed: {}", ex.getMessage());
            context.setStatusCode(401);
            return ResponseEntity.status(401).build();
        }
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
    public ResponseEntity<Map<String, String>> logout(
        @RequestHeader(value = "Authorization", required = false) String authHeader,
        HttpServletRequest httpRequest,
        HttpServletResponse httpResponse
    ) {
        // Create request context
        RequestContext context = new RequestContext(httpRequest, httpResponse);

        // Get interceptor chain
        InterceptorChain chain = interceptorManager.createAuthChain();

        try {
            // Execute with interceptor chain
            Map<String, String> response = chain.executeChain(context, () -> {
                var result = new HashMap<String, String>();

                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    var token = authHeader.substring(7);
                    var removed = tokenStorageService.removeToken(token);

                    if (removed) {
                        result.put(MESSAGE_KEY, "Logout successful");
                    } else {
                        result.put(MESSAGE_KEY, "Token not found or already logged out");
                    }
                    return result;
                }

                result.put(MESSAGE_KEY, "Invalid Authorization header");
                context.setStatusCode(400);
                return result;
            });

            return ResponseEntity.ok(response);

        } catch (Exception ex) {
            log.error("Logout failed: {}", ex.getMessage());
            context.setStatusCode(500);
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        var response = new HashMap<String, String>();
        response.put("status", "Authentication service is running");
        return ResponseEntity.ok(response);
    }
}
