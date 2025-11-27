package com.example.hotelreservationsystem.dto;

import com.example.hotelreservationsystem.enums.OrderStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO for booking creation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {

    private Long orderId;
    private Long customerId;
    private Long roomId;
    private OrderStatus orderStatus;
    private BigDecimal totalPrice;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private LocalDate checkInDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private LocalDate checkOutDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private LocalDateTime createdAt;

    private String checkInCode;
}
