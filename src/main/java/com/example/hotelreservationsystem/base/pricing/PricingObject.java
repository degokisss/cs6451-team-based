package com.example.hotelreservationsystem.base.pricing;

/**
 * Subject interface for the pricing observer pattern.
 *
 * <p>Implementations keep track of observers per room type and notify them when
 * the price for that room type changes.</p>
 */
public interface PricingObject {
    /**
     * Attach an observer to a specific room type.
     *
     * @param roomType the room type identifier
     * @param observer the observer to be notified of price changes
     */
    void attach(Long roomType, PricingObserver observer);

    /**
     * Detach an observer from a specific room type.
     *
     * @param roomType the room type identifier
     * @param observer the observer to remove
     */
    void detach(Long roomType, PricingObserver observer);

    /**
     * Notify all observers registered for the given room type about a price change.
     *
     * @param roomType the room type identifier
     * @param price    the new price to send to observers
     */
    void notify(Long roomType, Float price);
}