package com.example.hotelreservationsystem.service;

import com.example.hotelreservationsystem.entity.Hotel;
import com.example.hotelreservationsystem.repository.HotelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HotelService {
    @Autowired
    private final HotelRepository hotelRepository;
    public HotelService(HotelRepository hotelRepository) {
        this.hotelRepository = hotelRepository;
    }

    public void saveAll(List<Hotel> hotels) {
        hotelRepository.saveAll(hotels);
    }

    public List<Hotel> findAll() {
        return hotelRepository.findAll();
    }
}
