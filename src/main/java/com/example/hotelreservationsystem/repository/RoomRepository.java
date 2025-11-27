package com.example.hotelreservationsystem.repository;

import com.example.hotelreservationsystem.entity.Room;
import com.example.hotelreservationsystem.enums.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByRoomType_IdAndRoomStatus(Long roomTypeId, RoomStatus roomStatus);
}
