package com.example.hotelreservationsystem.controllers;

import com.example.hotelreservationsystem.entity.Hotel;
import com.example.hotelreservationsystem.entity.Room;
import com.example.hotelreservationsystem.service.HotelService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class HotelController {

    @Autowired
    HotelService hotelService;

    @GetMapping("/hotel/mock")
    public List<Hotel> addRooms() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Hotel> hotels = List.of(mapper.readValue(ResourceUtils.getFile("classpath:hotels.json"), Hotel[].class));
            hotelService.saveAll(hotels);
            return hotels;
        } catch (Exception e) {
            System.console().printf(e.getMessage());
            return List.of();
        }
    }

    @GetMapping("/hotels/list")
    public List<Hotel> getHotels() {
        try {
            return hotelService.findAll();
        } catch (Exception e) {
            System.console().printf(e.getMessage());
            return List.of();
        }
    }
}