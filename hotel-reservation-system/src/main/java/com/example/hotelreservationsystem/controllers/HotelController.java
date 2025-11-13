package com.example.hotelreservationsystem.controllers;

import com.example.hotelreservationsystem.dto.HotelCreateRequest;
import com.example.hotelreservationsystem.dto.HotelCreateResponse;
import com.example.hotelreservationsystem.entity.Hotel;
import com.example.hotelreservationsystem.service.HotelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api/hotel")
public class HotelController {

    @Autowired
    HotelService hotelService;

    @PostMapping
    public ResponseEntity<HotelCreateResponse> addHotel(@RequestBody HotelCreateRequest hotelCreateRequest) {
        var response = hotelService.create(hotelCreateRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/hotels/list")
    public List<Hotel> getHotels() {
        try {
            return hotelService.findAll();
        } catch (Exception e) {
            log.error(e.getMessage());
            return List.of();
        }
    }
}