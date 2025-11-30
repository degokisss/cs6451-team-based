package com.example.hotelreservationsystem.base.pricing;

/**
 * Component contract for the pricing system.
 *
 * <p>Implementations compute and return the effective price for a room, potentially
 * applying one or more decorators that alter the base price (membership discounts,
 * occupancy multipliers, holiday surcharges, etc.).</p>
 */
public interface PricingComponent {
    /**
     * Calculate the effective price.
     *
     * @return the calculated price as a {@link Float}
     */
    Float calc();
}