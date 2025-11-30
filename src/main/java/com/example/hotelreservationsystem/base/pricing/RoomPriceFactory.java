package com.example.hotelreservationsystem.base.pricing;

import com.example.hotelreservationsystem.entity.Customer;
import com.example.hotelreservationsystem.entity.Room;
import com.example.hotelreservationsystem.enums.RoomPriceTier;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoomPriceFactory {

    // Example method demonstrating how to create a pricing component with multiple tiers
    // This method is for illustration; actual implementation may vary based on requirements
    // and how tiers are applied.
    // Parameters:
    // - priceTiers: List of pricing tiers to apply
    // - price: The base price for the room
    public PricingComponent createPriceComponent1(List<RoomPriceTier> priceTiers, float price) {
        PricingComponent component = new RoomPricing(price);
        for (RoomPriceTier tier : priceTiers) {
            component = switch (tier) {
                case OCCUPANCY -> new OccupancyPricingDecorator(component, 0.5f);
                case HOLIDAY -> new HolidayPricingDecorator(component);
                default -> component;
            };
        }
        return component;
    }

    // Main method to create a pricing component based on a single price tier
    // This is the method currently used in the implementation
    // It selects the appropriate pricing strategy based on the provided price tier
    // and applies it to the base price component.
    // Parameters:
    // - priceTier: The pricing tier to apply (e.g., NORMAL_ORDER, OCC
    // - customer: The customer for whom the pricing is being calculated
    // - room: The room for which the pricing is being calculated
    // - occupancy: The occupancy rate, used for OCCUPANCY pricing tier
    public PricingComponent createPriceComponent(RoomPriceTier priceTier, Customer customer, Room room, float occupancy) {
        return switch (priceTier) {
            case OCCUPANCY -> createOccupancyPriceComponent(customer, room, occupancy);
            case HOLIDAY -> createHolidayPriceComponent(customer, room);
            default -> createNormalOrderPriceComponent(customer, room);
        };
    }

    // Helper method to create a normal order pricing component
    // This method applies the base room price and membership pricing decorator
    // Parameters:
    // - customer: The customer for whom the pricing is being calculated
    // - room: The room for which the pricing is being calculated
    private PricingComponent createNormalOrderPriceComponent(Customer customer, Room room) {
        PricingComponent component = new RoomPricing(room.getRoomType().getPrice());
        component = new MembershipPricingDecorator(component, customer.getMembershipTier());
        return component;
    }

    // Helper method to create an occupancy-based pricing component
    // This method first creates a normal order pricing component and then
    // applies the occupancy pricing decorator
    // Parameters:
    // - customer: The customer for whom the pricing is being calculated
    // - room: The room for which the pricing is being calculated
    // - occupancy: The occupancy rate to be used in the occupancy pricing calculation
    private PricingComponent createOccupancyPriceComponent(Customer customer, Room room, float occupancy) {
        var component = createNormalOrderPriceComponent(customer, room);
        component = new OccupancyPricingDecorator(component, occupancy);
        return component;
    }

    // Helper method to create a holiday pricing component
    // This method first creates a normal order pricing component and then
    // applies the holiday pricing decorator
    // Parameters:
    // - customer: The customer for whom the pricing is being calculated
    // - room: The room for which the pricing is being calculated
    private PricingComponent createHolidayPriceComponent(Customer customer, Room room) {
        var component = createNormalOrderPriceComponent(customer, room);
        component = new HolidayPricingDecorator(component);
        return component;
    }

}
