package com.example.hotelreservationsystem.dto;

import com.example.hotelreservationsystem.enums.RoomStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomForHotelCreateRequest {
    @NotNull(message = "Room type id cannot be null")
    private Long roomTypeId;

    @NotNull(message = "Room number cannot be null")
    @NotEmpty(message = "Room number cannot be empty")
    private String roomNumber;

    private RoomStatus roomStatus;
}
