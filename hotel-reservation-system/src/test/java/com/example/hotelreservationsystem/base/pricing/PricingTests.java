package com.example.hotelreservationsystem.base.pricing;

import com.example.hotelreservationsystem.entity.Customer;
import com.example.hotelreservationsystem.entity.Room;
import com.example.hotelreservationsystem.entity.RoomType;
import com.example.hotelreservationsystem.enums.MembershipTier;
import com.example.hotelreservationsystem.enums.RoomPriceTier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class PricingTests {
    @Autowired
    RoomPriceFactory roomPriceFactory;

    @Test
    public void testMembershipTierPrice() {

        for(var membershipTier : MembershipTier.values()) {
            PricingComponent pricingComponent = new RoomPricing(500f);
            pricingComponent = new MembershipPricingDecorator(pricingComponent, membershipTier);

            var price = pricingComponent.calc();

            assertEquals(switch (membershipTier) {
                case BRONZE -> 500 * 0.9f;
                case GOLD -> 500 * 0.80f;
                default -> 500f;
            }, price);
        }

    }

    @Test
    public void testHolidayChristmasPrice() {
        PricingComponent pricingComponent = new RoomPricing(500f);
        pricingComponent = new HolidayPricingDecorator(pricingComponent);

        assertEquals(500 * 1.5f, pricingComponent.calc());
    }

    @Test
    public void testOccupancyPrice() {
        var occupancies = new float[]{0.6f, 0.7f, 0.8f, 0.9f};
        for (var occupancy : occupancies) {
            PricingComponent pricingComponent = new RoomPricing(500f);
            pricingComponent = new OccupancyPricingDecorator(pricingComponent, occupancy);

            var rate = 1f;
            if (occupancy == 0.9f) {
                rate = 1.4f;
            } else if (occupancy == 0.8f) {
                rate = 1.3f;
            } else if (occupancy == 0.7f) {
                rate = 1.2f;
            }
            assertEquals(500 * rate, pricingComponent.calc());
        }
    }

    @Test
    public void testOccupancyPriceComponent() {
        var user = Customer
                .builder()
                .name("John")
                .membershipTier(MembershipTier.BRONZE)
                .build();
        var room = Room
                .builder()
                .roomType(
                        RoomType
                            .builder()
                            .price(500f)
                            .build()
                )
                .build();

        var component = roomPriceFactory.createPriceComponent(RoomPriceTier.OCCUPANCY, user, room, 0.8f);
        assertEquals(500f * 1.3f * 0.9f, component.calc());
    }

    @Test
    public void testNormalPriceComponent() {

        var user = Customer
                .builder()
                .name("John")
                .membershipTier(MembershipTier.GOLD)
                .build();
        var room = Room
                .builder()
                .roomType(
                        RoomType
                                .builder()
                                .price(400f)
                                .build()
                )
                .build();

        var component = roomPriceFactory.createPriceComponent(RoomPriceTier.NORMAL, user, room, 0.8f);
        assertEquals(400f * 0.8f, component.calc());
    }

    @Test
    public void testHolidayPriceComponentComponent() {
        var user = Customer
                .builder()
                .name("John")
                .membershipTier(MembershipTier.SILVER)
                .build();
        var room = Room
                .builder()
                .roomType(
                        RoomType
                                .builder()
                                .price(600f)
                                .build()
                )
                .build();
        var component = roomPriceFactory.createPriceComponent(RoomPriceTier.HOLIDAY, user, room, 0.8f);
        assertEquals(600f * 1.5f, component.calc());
    }


}
