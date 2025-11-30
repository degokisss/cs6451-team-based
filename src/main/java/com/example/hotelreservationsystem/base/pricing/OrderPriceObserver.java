package com.example.hotelreservationsystem.base.pricing;

import com.example.hotelreservationsystem.entity.Order;
import com.example.hotelreservationsystem.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Observer wrapper for Order entities
 * Handles price updates and persistence when room prices change
 * Part of Observer Pattern implementation for dynamic pricing
 */
@RequiredArgsConstructor
@Slf4j
public class OrderPriceObserver implements PricingObserver {

    private final Order order;
    private final OrderRepository orderRepository;

    /**
     * Called when room price changes
     * Updates the order's total price and persists to database
     *
     * @param newPrice The new room price per night
     */
    @Override
    public void update(float newPrice) {
        try {
            log.info("Price update notification for Order ID: {} - New price: ${}",
                order.getId(), newPrice);

            // Update order's total price using its Observer method
            order.update(newPrice);

            // Persist the updated order
            orderRepository.save(order);

            log.info("Order ID: {} updated successfully - New total price: ${}",
                order.getId(), order.getTotalPrice());
        } catch (Exception e) {
            log.error("Failed to update price for Order ID: {} - Error: {}",
                order.getId(), e.getMessage(), e);
        }
    }
}