package com.example.hotelreservationsystem.dto;

import com.example.hotelreservationsystem.enums.PaymentType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentRequest {
    @NotNull(message = "Order ID cannot be null")
    private Long orderId; // 只需要知道它是个 Long/ID 类型

    @NotNull(message = "Payment Type cannot be null")
    private PaymentType paymentType;


}
