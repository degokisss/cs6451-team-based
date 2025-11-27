package com.example.hotelreservationsystem.dto;

import lombok.Builder;
@Builder
public record LoginResponse (String token, String email, String name) {
}
