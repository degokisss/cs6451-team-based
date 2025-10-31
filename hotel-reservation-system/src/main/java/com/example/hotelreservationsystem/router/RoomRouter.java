package com.example.hotelreservationsystem.router;

import com.example.hotelreservationsystem.entity.Room;
import com.example.hotelreservationsystem.entity.RoomStatus;
import com.example.hotelreservationsystem.entity.RoomType;
import com.example.hotelreservationsystem.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class RoomRouter {

    @Autowired
    private RoomRepository roomRepository;

    @GetMapping("rooms/search")
    public List<Room> searchRooms(@RequestParam int roomType){
        return roomRepository.findByRoomTypeAndRoomStatus(RoomType.values()[roomType], RoomStatus.VANCANT);
    }


    @GetMapping("/rooms/list")
    public List<Room> getRooms() {
        try {
            return roomRepository.findAll();
        } catch (Exception e) {
            System.console().printf(e.getMessage());
            return List.of();
        }
    }

}
