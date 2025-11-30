package com.example.hotelreservationsystem.base.pricing;

/**
 * Decorator that adjusts the price based on current occupancy.
 *
 * <p>The higher the occupancy, the higher the multiplier applied to the underlying
 * pricing component.</p>
 */
class OccupancyPricingDecorator extends PricingDecorator {
    private final float occupancy;

    public OccupancyPricingDecorator(PricingComponent pricingComponent, float occupancy) {
        super(pricingComponent);
        this.occupancy = occupancy;
    }

    @Override
    public Float calc() {
        var rate = getRatesDueToOccupancy();
        return super.calc() * rate;
    }

    /**
     * Determine multiplier based on occupancy level.
     *
     * @return multiplier to be applied to base price
     */
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