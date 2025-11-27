package com.example.hotelreservationsystem.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a booking lock
 * Customer ID is extracted from JWT token (authenticated user)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingLockRequest {

    @NotNull(message = "Room ID is required")
    private Long roomId;
}
