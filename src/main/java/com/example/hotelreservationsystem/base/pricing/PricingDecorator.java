package com.example.hotelreservationsystem.base.pricing;

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
