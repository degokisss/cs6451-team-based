package com.example.hotelreservationsystem.service;

import com.example.hotelreservationsystem.entity.Room;
import com.example.hotelreservationsystem.entity.RoomStatus;
import com.example.hotelreservationsystem.entity.RoomType;
import com.example.hotelreservationsystem.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoomService {
    @Autowired
    private final RoomRepository roomRepository;
    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public List<Room> searchedRooms(int roomType) {
        return roomRepository.findByRoomTypeAndRoomStatus(RoomType.values()[roomType], RoomStatus.VANCANT);
    }

    public List<Room> findAll() {
        return roomRepository.findAll();
    }


}
