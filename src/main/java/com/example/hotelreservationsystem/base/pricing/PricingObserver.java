package com.example.hotelreservationsystem.base.pricing;

/**
 * Observer interface for receiving price updates.
 *
 * <p>Implementations react to changes in room price (for example by updating orders
 * or recalculating totals).</p>
 */
public interface PricingObserver {
    /**
     * Called when a price changes.
     *
     * @param price the new price value
     */
    void update(float price);
}