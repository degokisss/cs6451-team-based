package com.example.hotelreservationsystem.base.pricing;

class HolidayPricingDecorator extends PricingDecorator {

    public HolidayPricingDecorator(PricingComponent pricingComponent) {
        super(pricingComponent);
    }

    @Override
    public Float calc() {
        float holidayRate = 1.5f;
        return super.calc() * holidayRate;
    }
}
