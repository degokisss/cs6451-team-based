package com.example.hotelreservationsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
    private String token;
    @Builder.Default
    private String type = "Bearer";
    private String email;
    private String name;

    public LoginResponse(String token, String email, String name) {
        this.token = token;
        this.type = "Bearer";
        this.email = email;
        this.name = name;
    }
}
