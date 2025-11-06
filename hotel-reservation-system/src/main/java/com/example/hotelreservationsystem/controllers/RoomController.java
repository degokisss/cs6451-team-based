package com.example.hotelreservationsystem.controllers;

import com.example.hotelreservationsystem.entity.Room;
import com.example.hotelreservationsystem.service.RoomService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Slf4j
public class RoomController {

    @Autowired
    private RoomService roomService;

    @GetMapping("rooms/search")
    public List<Room> searchRooms(@RequestParam Long roomTypeId){
        return roomService.searchedRooms(roomTypeId);
    }


    @GetMapping("/rooms/list")
    public List<Room> getRooms() {
        try {
            return roomService.findAll();
        } catch (Exception e) {
            log.error(e.getMessage());
            return List.of();
        }
    }

}
