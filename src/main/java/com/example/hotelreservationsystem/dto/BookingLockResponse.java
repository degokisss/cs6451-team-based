package com.example.hotelreservationsystem.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingLockResponse {

    private String lockId;
    private Long roomId;
    private Long customerId;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private LocalDateTime expiresAt;
}
