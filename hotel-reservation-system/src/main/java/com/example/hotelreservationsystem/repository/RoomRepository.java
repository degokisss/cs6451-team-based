package com.example.hotelreservationsystem.repository;

import com.example.hotelreservationsystem.entity.Room;
import com.example.hotelreservationsystem.entity.RoomStatus;
import com.example.hotelreservationsystem.entity.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByRoomTypeAndRoomStatus(RoomType roomType, RoomStatus roomStatus);

}
