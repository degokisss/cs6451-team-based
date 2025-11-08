package com.example.hotelreservationsystem.base.pricing;

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
