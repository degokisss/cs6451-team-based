package com.example.hotelreservationsystem.repository;

import com.example.hotelreservationsystem.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {
}
