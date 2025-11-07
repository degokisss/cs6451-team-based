package com.example.hotelreservationsystem.base.pricing;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class PricingObserverTest {

    @Autowired
    RoomPriceService roomPriceService;

    @Test
    public void testObserver() {
        roomPriceService.addObserver("SINGLE", price -> Assertions.assertEquals(100f, price));
        roomPriceService.addObserver("SINGLE", price -> Assertions.assertEquals(100f, price));

        roomPriceService.addObserver("HOLIDAY", price -> Assertions.assertEquals(200f, price));

        roomPriceService.updatePrice("SINGLE", 100f);
        roomPriceService.updatePrice("HOLIDAY", 200f);
    }
}
