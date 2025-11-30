package com.example.hotelreservationsystem.base.pricing;

public interface PricingObject {
    void attach(Long roomType, PricingObserver observer);

    void detach(Long roomType, PricingObserver observer);

    void notify(Long roomType, Float price);
}
