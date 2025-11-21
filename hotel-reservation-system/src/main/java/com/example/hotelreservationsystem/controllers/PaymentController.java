package com.example.hotelreservationsystem.controllers;

import com.example.hotelreservationsystem.dto.PaymentRequest;
import com.example.hotelreservationsystem.dto.PaymentResponse;
import com.example.hotelreservationsystem.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping("/execute")
    public ResponseEntity<PaymentResponse> executePayment(@RequestBody @Validated PaymentRequest request) {

        // 1. call Service core logic
        boolean success = paymentService.executePayment(request.getOrderId(), request.getPaymentType());

        // 2. build response
        PaymentResponse response = PaymentResponse.builder()
                .orderId(request.getOrderId())
                .status(success ? "PAID" : "FAILED")
                .transactionId("MOCK_TXN_" + request.getOrderId())
                .message(success ? "Payment processed successfully." : "Payment failed. Please try again.")
                .build();

        return ResponseEntity.ok(response);
    }
}