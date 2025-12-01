package com.example.hotelreservationsystem.base.pricing;

/**
 * Base decorator that forwards calculation to the wrapped {@link PricingComponent}.
 *
 * <p>Concrete decorators extend this class to modify the result of {@link #calc()}.</p>
 */
class PricingDecorator implements PricingComponent {
    private final PricingComponent pricingComponent;
    public PricingDecorator(PricingComponent pricingComponent) {
        this.pricingComponent = pricingComponent;
    }
    @Override
    public Float calc() {
        return pricingComponent.calc();
    }
}