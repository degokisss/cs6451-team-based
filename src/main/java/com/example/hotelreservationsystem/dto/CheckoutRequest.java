package com.example.hotelreservationsystem.dto;

import lombok.Builder;

@Builder
public record CheckoutRequest(Long roomId) {
}
