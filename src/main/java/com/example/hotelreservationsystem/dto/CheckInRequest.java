package com.example.hotelreservationsystem.dto;

import lombok.Builder;

@Builder
public record CheckInRequest(Long orderId, String checkInCode) {
}