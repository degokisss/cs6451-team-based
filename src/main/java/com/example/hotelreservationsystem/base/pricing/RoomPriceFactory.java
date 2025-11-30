package com.example.hotelreservationsystem.base.pricing;

import com.example.hotelreservationsystem.entity.Customer;
import com.example.hotelreservationsystem.entity.Room;
import com.example.hotelreservationsystem.enums.RoomPriceTier;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Factory for creating {@link PricingComponent} instances based on pricing tiers
 * and context such as the room and customer.
 *
 * <p>Provides helper methods that compose decorators (membership, occupancy, holiday)
 * around the base {@link RoomPricing} component.</p>
 */
@Service
public class RoomPriceFactory {

    /**
     * Example method demonstrating how to create a pricing component with multiple tiers.
     * This method is for illustration; actual implementation may vary based on requirements
     * and how tiers are applied.
     *
     * @param priceTiers the list of tiers to apply
     * @param price      the base price for the room
     * @return a composed {@link PricingComponent} applying the requested tiers
     */
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

    /**
     * Create a pricing component based on a single price tier and context.
     *
     * @param priceTier the pricing tier to apply
     * @param customer  the customer for pricing decisions (membership tier)
     * @param room      the room whose base price will be used
     * @param occupancy the occupancy rate used when applying occupancy pricing
     * @return a composed {@link PricingComponent}
     */
    public PricingComponent createPriceComponent(RoomPriceTier priceTier, Customer customer, Room room, float occupancy) {
        return switch (priceTier) {
            case OCCUPANCY -> createOccupancyPriceComponent(customer, room, occupancy);
            case HOLIDAY -> createHolidayPriceComponent(customer, room);
            default -> createNormalOrderPriceComponent(customer, room);
        };
    }

    /**
     * Helper method to create a normal order pricing component (base + membership decorator).
     */
    private PricingComponent createNormalOrderPriceComponent(Customer customer, Room room) {
        PricingComponent component = new RoomPricing(room.getRoomType().getPrice());
        component = new MembershipPricingDecorator(component, customer.getMembershipTier());
        return component;
    }

    /**
     * Helper method to create an occupancy-based pricing component.
     */
    private PricingComponent createOccupancyPriceComponent(Customer customer, Room room, float occupancy) {
        var component = createNormalOrderPriceComponent(customer, room);
        component = new OccupancyPricingDecorator(component, occupancy);
        return component;
    }

    /**
     * Helper method to create a holiday pricing component.
     */
    private PricingComponent createHolidayPriceComponent(Customer customer, Room room) {
        var component = createNormalOrderPriceComponent(customer, room);
        component = new HolidayPricingDecorator(component);
        return component;
    }

}