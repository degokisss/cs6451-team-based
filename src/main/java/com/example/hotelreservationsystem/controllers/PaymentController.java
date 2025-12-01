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
        // Call service
        PaymentResponse response = paymentService.executePayment(request.getOrderId(), request.getPaymentType());

        if ("SUCCESS".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
}