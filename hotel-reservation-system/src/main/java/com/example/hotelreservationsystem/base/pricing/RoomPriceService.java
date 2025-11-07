package com.example.hotelreservationsystem.base.pricing;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RoomPriceService {

    Map<String, List<PricingObserver>> _dependencies = new HashMap<>();

    public void addObserver(String roomType, PricingObserver observer) {
        _dependencies.computeIfAbsent(roomType, s -> new ArrayList<>()).add(observer);
    }

    // String: roomType, Float: price
    private final Map<String, Float> _prices = new HashMap<>();

    public void removeObserver(String roomType, PricingObserver observer) {
        List<PricingObserver> observers = _dependencies.get(roomType);
        if (observers != null) {
            observers.remove(observer);
        }
    }

    public void notifyObservers(String roomType, Float price) {
        List<PricingObserver> observers = _dependencies.get(roomType);
        if (observers != null) {
            for (PricingObserver observer : observers) {
                observer.update(price);
            }
        }
    }

    public void updatePrice(String roomType, float price) {
        Float oldPrice = _prices.get(roomType);
        if (oldPrice == null || oldPrice != price) {
            _prices.put(roomType, price);
            notifyObservers(roomType, price);
        }
    }
}
