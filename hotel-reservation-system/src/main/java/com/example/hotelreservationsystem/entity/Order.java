package com.example.hotelreservationsystem.entity;

import com.example.hotelreservationsystem.base.pricing.PricingObserver;
import com.example.hotelreservationsystem.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Order entity representing a confirmed booking
 * Implements State Pattern through OrderStatus enum
 * Implements Observer Pattern to observe room price changes
 */
@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true, exclude = {"customer", "room"})
@EqualsAndHashCode(callSuper = true, exclude = {"customer", "room"})
@Entity
@Table(
    name = "\"order\"",  // Quoted because "order" is a SQL reserved keyword
    indexes = {
        @Index(name = "idx_order_customer", columnList = "customer_id"),
        @Index(name = "idx_order_room", columnList = "room_id"),
        @Index(name = "idx_order_status", columnList = "order_status")
    }
)
@Builder
@AllArgsConstructor
public class Order extends BaseEntityAudit implements PricingObserver {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "check_out_date", nullable = false)
    private LocalDate checkOutDate;

    @Column(name = "number_of_nights", nullable = false)
    private Long numberOfNights;

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "order_status", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private OrderStatus orderStatus = OrderStatus.PENDING;

    @Column(name = "check_in_code", unique = true, length = 8)
    private String checkInCode;

    /**
     * Observer Pattern implementation
     * Called when room price changes to update order's total price
     *
     * @param newPrice The new room price per night
     */
    @Override
    public void update(float newPrice) {
        BigDecimal newRoomPrice = BigDecimal.valueOf(newPrice);
        this.totalPrice = newRoomPrice.multiply(BigDecimal.valueOf(numberOfNights));
    }
}
