package com.example.hotelreservationsystem.router;

import com.example.hotelreservationsystem.entity.Hotel;
import com.example.hotelreservationsystem.entity.Room;
import com.example.hotelreservationsystem.repository.HotelRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class HotelRouter {

    @Autowired HotelRepository hotelRepository;

    @GetMapping("/hotel/mock")
    public List<Hotel> addRooms() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Hotel> hotels = List.of(mapper.readValue(ResourceUtils.getFile("classpath:hotels.json"), Hotel[].class));
            for (Hotel hotel : hotels) {
                for (Room room: hotel.getRooms()) {
                    room.setHotel(hotel);
                }
            }
            hotelRepository.saveAll(hotels);
            return hotels;
        } catch (Exception e) {
            System.console().printf(e.getMessage());
            return List.of();
        }
    }

    @GetMapping("/hotels")
    public List<Hotel> getHotels() {
        try {
            return hotelRepository.findAll();
        } catch (Exception e) {
            System.console().printf(e.getMessage());
            return List.of();
        }
    }
}