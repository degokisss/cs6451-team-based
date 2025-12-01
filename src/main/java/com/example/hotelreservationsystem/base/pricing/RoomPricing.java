package com.example.hotelreservationsystem.base.pricing;

/**
 * Basic pricing component representing the raw room price.
 *
 * <p>This class is the concrete component in the pricing component hierarchy
 * and simply returns the original room price passed to it.</p>
 */
class RoomPricing implements PricingComponent {
    private final Float originalPrice;

    RoomPricing(Float price) {
        this.originalPrice = price;
    }

    @Override
    public Float calc() {
        return originalPrice;
    }
}