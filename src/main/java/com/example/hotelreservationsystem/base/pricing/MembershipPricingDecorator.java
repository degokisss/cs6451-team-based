package com.example.hotelreservationsystem.base.pricing;

import com.example.hotelreservationsystem.enums.MembershipTier;

/**
 * Decorator that applies a membership discount based on the customer's tier.
 */
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

    /**
     * Resolve discount multiplier for a membership tier.
     */
    private float membershipStrategy(MembershipTier membershipTier) {
        return switch (membershipTier) {
            case BRONZE -> 0.90f;
            case GOLD -> 0.80f;
            default -> 1f;
        };
    }
}