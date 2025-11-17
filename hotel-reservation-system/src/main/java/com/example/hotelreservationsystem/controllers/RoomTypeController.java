package com.example.hotelreservationsystem.controllers;

import com.example.hotelreservationsystem.dto.RoomTypeCreateRequest;
import com.example.hotelreservationsystem.entity.RoomType;
import com.example.hotelreservationsystem.repository.RoomTypeRepository;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api/room-types")
public class RoomTypeController {

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @PostMapping
    public ResponseEntity<RoomType> create(@RequestBody @Valid RoomTypeCreateRequest req) {
        var rt = RoomType.builder()
                .name(req.getName())
                .description(req.getDescription())
                .price(req.getPrice() == null ? 0f : req.getPrice())
                .capacity(req.getCapacity() == null ? 0 : req.getCapacity())
                .build();

        var saved = roomTypeRepository.save(rt);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/list")
    public List<RoomType> list() {
        return roomTypeRepository.findAll();
    }
}
