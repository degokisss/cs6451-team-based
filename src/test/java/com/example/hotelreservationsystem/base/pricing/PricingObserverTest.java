package com.example.hotelreservationsystem.base.pricing;

import com.example.hotelreservationsystem.service.RoomService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class PricingObserverTest {

    @Autowired
    RoomService roomService;

    @Test
    public void testObserver() {
        roomService.attach(1L, price -> assertEquals(100f, price));
        roomService.attach(1L, price -> assertEquals(100f, price));

        roomService.attach(2L, price -> assertEquals(200f, price));

        roomService.updatePrice(1L, 100f);
        roomService.updatePrice(2L, 200f);
    }
}
