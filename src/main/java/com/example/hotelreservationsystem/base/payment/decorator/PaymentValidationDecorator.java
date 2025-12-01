package com.example.hotelreservationsystem.base.payment.decorator;

import com.example.hotelreservationsystem.base.payment.PaymentStrategy;
import com.example.hotelreservationsystem.dto.PaymentRequest;
import com.example.hotelreservationsystem.dto.PaymentResponse;
import com.example.hotelreservationsystem.entity.Order;
import com.example.hotelreservationsystem.enums.OrderStatus;
import com.example.hotelreservationsystem.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class PaymentValidationDecorator extends PaymentStrategyDecorator {

    private final OrderRepository orderRepository;

    // Constructor: Inject OrderRepository to access the database
    public PaymentValidationDecorator(PaymentStrategy wrappedStrategy, OrderRepository orderRepository) {
        super(wrappedStrategy);
        this.orderRepository = orderRepository;
    }

    @Override
    public PaymentResponse pay(PaymentRequest request) {
        log.info("Validating payment request for Order ID: {}", request.getOrderId());

        if (request.getOrderId() == null) {
            log.error("Validation failed: Order ID is missing.");
            throw new IllegalArgumentException("Order ID cannot be null");
        }

        // Verify if the order exists in the database
        Optional<Order> orderOpt = orderRepository.findById(request.getOrderId());
        if (orderOpt.isEmpty()) {
            log.error("Validation failed: Order {} not found in database.", request.getOrderId());
            throw new IllegalArgumentException("Order not found");
        }

        Order order = orderOpt.get();

        //  Prevent duplicate payments
        //  only allow payment if the order status is 'PENDING'.
        if (order.getOrderStatus() != OrderStatus.PENDING) {
            log.warn("Validation failed: Order {} is already {}. Payment rejected.",
                    request.getOrderId(), order.getOrderStatus());

            // Stop the process if the order is already paid or cancelled
            throw new IllegalStateException("Order is not in PENDING status.");
        }

        log.info("Validation passed. Proceeding to payment logic.");

        // Validation is successful, pass to the next layer (Retry Logic -> Real Payment)
        return super.pay(request);
    }
}