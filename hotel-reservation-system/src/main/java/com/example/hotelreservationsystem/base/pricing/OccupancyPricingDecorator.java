package com.example.hotelreservationsystem.base.pricing;

class OccupancyPricingDecorator extends PricingDecorator {
    private final float occupancy;

    public OccupancyPricingDecorator(PricingComponent pricingComponent, float occupancy) {
        this.occupancy = occupancy;
        super(pricingComponent);
    }

    @Override
    public Float calc() {
        var rate = getRatesDueToOccupancy();
        return super.calc() * rate;
    }

    private float getRatesDueToOccupancy() {
        if (occupancy >= 0.9f) {
            return 1.4f;
        } else if (occupancy >= 0.8f) {
            return 1.3f;
        } else if (occupancy >= 0.7f) {
            return 1.2f;
        } else {
            return 1f;
        }
    }
}
