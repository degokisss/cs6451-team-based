package com.example.hotelreservationsystem.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/oauth2")
public class OAuth2Controller {

    @GetMapping("/redirect")
    public ResponseEntity<Map<String, Object>> oauth2Redirect(@RequestParam String token) {
        var response = new HashMap<String, Object>();
        response.put("token", token);
        response.put("type", "Bearer");
        response.put("message", "OAuth2 authentication successful. Use this token in the Authorization header for API requests.");
        return ResponseEntity.ok(response);
    }
}
