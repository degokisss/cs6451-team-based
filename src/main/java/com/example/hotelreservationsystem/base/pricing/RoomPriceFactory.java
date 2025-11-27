package com.example.hotelreservationsystem.base.pricing;

import com.example.hotelreservationsystem.entity.Customer;
import com.example.hotelreservationsystem.entity.Room;
import com.example.hotelreservationsystem.enums.RoomPriceTier;
import org.springframework.stereotype.Service;

@Service
public class RoomPriceFactory {
    public PricingComponent createPriceComponent(RoomPriceTier priceTier, Customer customer, Room room, float occupancy) {
        return switch (priceTier) {
            case OCCUPANCY -> createOccupancyPriceComponent(customer, room, occupancy);
            case HOLIDAY -> createHolidayPriceComponent(customer, room);
            default -> createNormalOrderPriceComponent(customer, room);
        };
    }

    private PricingComponent createNormalOrderPriceComponent(Customer customer, Room room) {
        PricingComponent component = new RoomPricing(room.getRoomType().getPrice());
        component = new MembershipPricingDecorator(component, customer.getMembershipTier());
        return component;
    }

    private PricingComponent createOccupancyPriceComponent(Customer customer, Room room, float occupancy) {
        var component = createNormalOrderPriceComponent(customer, room);
        component = new OccupancyPricingDecorator(component, occupancy);
        return component;
    }

    private PricingComponent createHolidayPriceComponent(Customer customer, Room room) {
        var component = createNormalOrderPriceComponent(customer, room);
        component = new HolidayPricingDecorator(component);
        return component;
    }

}
