package com.example.hotelreservationsystem.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentResponse {
    private Long orderId;
    private String status; // 例如：SUCCESS, PENDING, FAILED
    private String transactionId;
    private String message;
}