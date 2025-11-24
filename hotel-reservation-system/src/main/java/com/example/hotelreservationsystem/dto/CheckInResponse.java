package com.example.hotelreservationsystem.dto;

import lombok.Builder;

@Builder
public record CheckInResponse(Long orderId, String status) {

}
