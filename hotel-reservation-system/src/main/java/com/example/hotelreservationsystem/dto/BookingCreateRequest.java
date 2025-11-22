package com.example.hotelreservationsystem.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO for creating a booking from a lock
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingCreateRequest {

    @NotBlank(message = "Lock ID is required")
    private String lockId;

    @NotNull(message = "Room ID is required")
    private Long roomId;

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @NotBlank(message = "Guest name is required")
    private String guestName;

    @NotBlank(message = "Guest email is required")
    @Email(message = "Guest email must be valid")
    private String guestEmail;

    @NotBlank(message = "Guest phone is required")
    private String guestPhone;

    @NotNull(message = "Check-in date is required")
    @Future(message = "Check-in date must be in the future")
    private LocalDate checkInDate;

    @NotNull(message = "Check-out date is required")
    @Future(message = "Check-out date must be in the future")
    private LocalDate checkOutDate;
}
