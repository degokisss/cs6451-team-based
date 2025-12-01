package com.example.hotelreservationsystem.dto;

import com.example.hotelreservationsystem.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for booking cancellation
 * Contains cancellation confirmation details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancellationResponse {

    private Long orderId;

    private OrderStatus previousStatus;

    private LocalDateTime cancelledAt;

    private String message;

    private String cancellationReason;
}
