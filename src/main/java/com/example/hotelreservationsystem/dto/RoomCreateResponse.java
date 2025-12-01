package com.example.hotelreservationsystem.dto;

import com.example.hotelreservationsystem.enums.RoomStatus;
import lombok.Builder;

@Builder
public record RoomCreateResponse(Long id, String roomNumber, Long hotelId, Long roomTypeId, RoomStatus roomStatus) {
}
