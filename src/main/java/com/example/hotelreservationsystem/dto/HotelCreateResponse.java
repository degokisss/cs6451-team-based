package com.example.hotelreservationsystem.dto;

import lombok.Builder;

@Builder
public record HotelCreateResponse(Long id, String name, String address) {
}
