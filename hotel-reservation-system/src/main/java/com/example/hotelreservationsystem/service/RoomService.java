package com.example.hotelreservationsystem.service;

import com.example.hotelreservationsystem.base.pricing.PricingObserver;
import com.example.hotelreservationsystem.entity.Room;
import com.example.hotelreservationsystem.enums.RoomStatus;
import com.example.hotelreservationsystem.repository.RoomRepository;
import com.example.hotelreservationsystem.repository.HotelRepository;
import com.example.hotelreservationsystem.repository.RoomTypeRepository;
import com.example.hotelreservationsystem.entity.RoomType;
import com.example.hotelreservationsystem.dto.RoomCreateRequest;
import com.example.hotelreservationsystem.dto.RoomCreateResponse;
import com.example.hotelreservationsystem.enums.RoomStatus;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class RoomService {
    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final RoomTypeRepository roomTypeRepository;

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

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<com.example.hotelreservationsystem.dto.RoomCreateResponse> findAllDto() {
        List<Room> rooms = roomRepository.findAll();
        return rooms.stream().map(room -> {
            Long hotelId = null;
            if (room.getHotel() != null) {
                try { hotelId = room.getHotel().getId(); } catch (Exception ignored) {}
            }

            Long roomTypeId = null;
            if (room.getRoomType() != null) {
                try { roomTypeId = room.getRoomType().getId(); } catch (Exception ignored) {}
            }

            return com.example.hotelreservationsystem.dto.RoomCreateResponse.builder()
                    .id(room.getId())
                    .roomNumber(room.getRoomNumber())
                    .hotelId(hotelId)
                    .roomTypeId(roomTypeId)
                    .roomStatus(room.getRoomStatus())
                    .build();
        }).collect(Collectors.toList());
    }

    public Room findById(Long id) {
        return roomRepository.findById(id).orElse(null);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public com.example.hotelreservationsystem.dto.RoomCreateResponse findDtoById(Long id) {
        var roomOpt = roomRepository.findById(id);
        if (roomOpt.isEmpty()) return null;
        var room = roomOpt.get();

        Long hotelId = null;
        if (room.getHotel() != null) {
            try {
                hotelId = room.getHotel().getId();
            } catch (Exception ignored) {
            }
        }

        Long roomTypeId = null;
        if (room.getRoomType() != null) {
            try {
                roomTypeId = room.getRoomType().getId();
            } catch (Exception ignored) {
            }
        }

        return com.example.hotelreservationsystem.dto.RoomCreateResponse.builder()
                .id(room.getId())
                .roomNumber(room.getRoomNumber())
                .hotelId(hotelId)
                .roomTypeId(roomTypeId)
                .roomStatus(room.getRoomStatus())
                .build();
    }

    @Transactional
    public RoomCreateResponse create(RoomCreateRequest request) {
        var hotel = hotelRepository.findById(request.getHotelId());
        if (hotel.isEmpty()) {
            return null; // controller will translate to 404
        }

        var roomTypeOpt = roomTypeRepository.findById(request.getRoomTypeId());
        if (roomTypeOpt.isEmpty()) {
            return null; // controller will translate to 404 when room type doesn't exist
        }

        var room = Room.builder()
                       .hotel(hotel.get())
                       .roomNumber(request.getRoomNumber())
                       .roomStatus(request.getRoomStatus() == null ? RoomStatus.VACANT : request.getRoomStatus())
                       .roomType(roomTypeOpt.get())
                       .build();

        roomRepository.save(room);

        return RoomCreateResponse.builder()
                                 .id(room.getId())
                                 .roomNumber(room.getRoomNumber())
                                 .hotelId(room.getHotel().getId())
                                 .roomTypeId(room.getRoomType().getId())
                                 .roomStatus(room.getRoomStatus())
                                 .build();
    }

    @Transactional
    public RoomCreateResponse update(Long id, RoomCreateRequest request) {
        var existing = roomRepository.findById(id);
        if (existing.isEmpty()) {
            return null;
        }

        var room = existing.get();

        if (request.getHotelId() != null && !request.getHotelId().equals(room.getHotel() == null ? null : room.getHotel().getId())) {
            var hotel = hotelRepository.findById(request.getHotelId());
            if (hotel.isEmpty()) {
                return null;
            }
            room.setHotel(hotel.get());
        }

        if (request.getRoomTypeId() != null) {
            var roomTypeOpt = roomTypeRepository.findById(request.getRoomTypeId());
            if (roomTypeOpt.isEmpty()) {
                return null; // treat missing room type as not-found
            }
            room.setRoomType(roomTypeOpt.get());
        }

        room.setRoomNumber(request.getRoomNumber());
        if (request.getRoomStatus() != null) room.setRoomStatus(request.getRoomStatus());

        roomRepository.save(room);

        return RoomCreateResponse.builder()
                                 .id(room.getId())
                                 .roomNumber(room.getRoomNumber())
                                 .hotelId(room.getHotel() == null ? null : room.getHotel().getId())
                                 .roomTypeId(room.getRoomType() == null ? null : room.getRoomType().getId())
                                 .roomStatus(room.getRoomStatus())
                                 .build();
    }

    @Transactional
    public boolean delete(Long id) {
        if (!roomRepository.existsById(id)) {
            return false;
        }
        roomRepository.deleteById(id);
        return true;
    }

    public void updatePrice(Long roomTypeId, float price) {
        _updatePrice(roomTypeId, price);

        // todo update db
    }

}
