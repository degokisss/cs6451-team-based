package com.example.hotelreservationsystem.router;

import com.example.hotelreservationsystem.entity.Room;
import com.example.hotelreservationsystem.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class RoomRouter {

    @Autowired
    private RoomRepository roomRepository;


    @GetMapping("/room/get")
    public List<Room> getRooms() {
        try {
            return roomRepository.findAll();
        } catch (Exception e) {
            System.console().printf(e.getMessage());
            return List.of();
        }
    }

}
