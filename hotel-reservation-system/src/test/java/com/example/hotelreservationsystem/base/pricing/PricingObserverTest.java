package com.example.hotelreservationsystem.base.pricing;

import com.example.hotelreservationsystem.service.RoomService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class PricingObserverTest {

    @Autowired
    RoomService roomService;

    @Test
    public void testObserver() {
        roomService.addObserver(1L, price -> Assertions.assertEquals(100f, price));
        roomService.addObserver(1L, price -> Assertions.assertEquals(100f, price));

        roomService.addObserver(2L, price -> Assertions.assertEquals(200f, price));

        roomService.updatePrice(1L, 100f);
        roomService.updatePrice(2L, 200f);
    }
}
