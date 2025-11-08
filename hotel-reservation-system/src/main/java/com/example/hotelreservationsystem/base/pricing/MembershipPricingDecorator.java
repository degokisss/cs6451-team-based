package com.example.hotelreservationsystem.base.pricing;

import com.example.hotelreservationsystem.enums.MembershipTier;

class MembershipPricingDecorator extends PricingDecorator {
    private final MembershipTier membershipTier;
    public MembershipPricingDecorator(PricingComponent pricingComponent, MembershipTier membershipTier) {
        super(pricingComponent);
        this.membershipTier = membershipTier;
    }

    @Override
    public Float calc() {
        var discount = membershipStrategy(membershipTier);
        return super.calc() * discount;
    }

    private float membershipStrategy(MembershipTier membershipTier) {
        return switch (membershipTier) {
            case BRONZE -> 0.90f;
            case GOLD -> 0.80f;
            default -> 1f;
        };
    }
}
