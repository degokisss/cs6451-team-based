package com.example.hotelreservationsystem.service;

import com.example.hotelreservationsystem.dto.HotelCreateRequest;
import com.example.hotelreservationsystem.dto.HotelCreateResponse;
import com.example.hotelreservationsystem.entity.Hotel;
import com.example.hotelreservationsystem.exception.HotelAlreadyExistsException;
import com.example.hotelreservationsystem.repository.HotelRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
public class HotelService {
    private final HotelRepository hotelRepository;

    public List<Hotel> findAll() {
        return hotelRepository.findAll();
    }

    @Transactional
    public HotelCreateResponse create(HotelCreateRequest hotelCreateRequest) {
        var isExisted = hotelRepository.existsByName(hotelCreateRequest.getName());

        if (isExisted)
            throw new HotelAlreadyExistsException(hotelCreateRequest.getName());

        var hotel = Hotel.builder()
                         .name(hotelCreateRequest.getName())
                         .address(hotelCreateRequest.getAddress())
                         .build();
        hotelRepository.save(hotel);

        return HotelCreateResponse.builder()
                                  .id(hotel.getId())
                                  .name(hotelCreateRequest.getName())
                                  .address(hotelCreateRequest.getAddress())
                                  .build();
    }

    public Hotel findById(Long id) {
        return hotelRepository.findById(id).orElse(null);
    }

    @Transactional
    public HotelCreateResponse update(Long id, HotelCreateRequest hotelCreateRequest) {
        var existing = hotelRepository.findById(id);
        if (existing.isEmpty()) {
            return null; // controller will translate to 404
        }

        var hotel = existing.get();

        // if updating name to one that exists on another hotel, prevent it
        if (!hotel.getName().equals(hotelCreateRequest.getName()) && hotelRepository.existsByName(hotelCreateRequest.getName())) {
            throw new HotelAlreadyExistsException(hotelCreateRequest.getName());
        }

        hotel.setName(hotelCreateRequest.getName());
        hotel.setAddress(hotelCreateRequest.getAddress());

        hotelRepository.save(hotel);

        return HotelCreateResponse.builder()
                                  .id(hotel.getId())
                                  .name(hotel.getName())
                                  .address(hotel.getAddress())
                                  .build();
    }

    @Transactional
    public boolean delete(Long id) {
        if (!hotelRepository.existsById(id)) {
            return false;
        }
        hotelRepository.deleteById(id);
        return true;
    }
}
