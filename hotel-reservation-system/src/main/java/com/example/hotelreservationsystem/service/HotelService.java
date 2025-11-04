package com.example.hotelreservationsystem.service;

import com.example.hotelreservationsystem.entity.Hotel;
import com.example.hotelreservationsystem.repository.HotelRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class HotelService {
    private final HotelRepository hotelRepository;

    public void saveAll(List<Hotel> hotels) {
        hotelRepository.saveAll(hotels);
    }

    public List<Hotel> findAll() {
        return hotelRepository.findAll();
    }
}
