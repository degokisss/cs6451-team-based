package com.example.hotelreservationsystem.controllers;

import com.example.hotelreservationsystem.entity.Room;
import com.example.hotelreservationsystem.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class RoomController {

    @Autowired
    private RoomService roomService;

    @GetMapping("rooms/search")
    public List<Room> searchRooms(@RequestParam int roomType){
        return roomService.searchedRooms(roomType);
    }


    @GetMapping("/rooms/list")
    public List<Room> getRooms() {
        try {
            return roomService.findAll();
        } catch (Exception e) {
            System.console().printf(e.getMessage());
            return List.of();
        }
    }

}
