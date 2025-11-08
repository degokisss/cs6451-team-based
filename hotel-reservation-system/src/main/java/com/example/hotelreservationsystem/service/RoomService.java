package com.example.hotelreservationsystem.service;

import com.example.hotelreservationsystem.base.pricing.PricingObserver;
import com.example.hotelreservationsystem.entity.Room;
import com.example.hotelreservationsystem.enums.RoomStatus;
import com.example.hotelreservationsystem.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class RoomService {
    private final RoomRepository roomRepository;

    private final ConcurrentHashMap<Long, List<PricingObserver>> _dependencies = new ConcurrentHashMap<>();

    // Long: roomType, Float: price
    private final ConcurrentHashMap<Long, Float> _prices = new ConcurrentHashMap<>();

    public void addObserver(Long roomType, PricingObserver observer) {
        _dependencies.computeIfAbsent(roomType, s -> new ArrayList<>()).add(observer);
    }

    public void removeObserver(Long roomType, PricingObserver observer) {
        List<PricingObserver> observers = _dependencies.get(roomType);
        if (observers != null) {
            observers.remove(observer);
        }
    }

    public synchronized void notifyObservers(Long roomType, Float price) {
        List<PricingObserver> observers = _dependencies.get(roomType);
        if (observers != null) {
            for (PricingObserver observer : observers) {
                observer.update(price);
            }
        }
    }

    public void _updatePrice(Long roomType, float price) {
        Float oldPrice = _prices.get(roomType);
        if (oldPrice == null || !oldPrice.equals(price)) {
            _prices.put(roomType, price);
            notifyObservers(roomType, price);
        }
    }


    public List<Room> searchedRooms(Long roomTypeId) {
        return roomRepository.findByRoomType_IdAndRoomStatus(roomTypeId, RoomStatus.VACANT);
    }

    public List<Room> findAll() {
        return roomRepository.findAll();
    }

    public void updatePrice(Long roomTypeId, float price) {
        _updatePrice(roomTypeId, price);

        // todo update db
    }

}
