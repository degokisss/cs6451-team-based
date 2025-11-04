package com.example.hotelreservationsystem.service;

import com.example.hotelreservationsystem.entity.Room;
import com.example.hotelreservationsystem.enums.RoomStatus;
import com.example.hotelreservationsystem.repository.RoomRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class RoomService {
    private final RoomRepository roomRepository;

    public List<Room> searchedRooms(Long roomTypeId) {
        return roomRepository.findByRoomType_IdAndRoomStatus(roomTypeId, RoomStatus.VACANT);
    }

    public List<Room> findAll() {
        return roomRepository.findAll();
    }
}
