package com.example.hotelreservationsystem.repository;

import com.example.hotelreservationsystem.entity.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HotelRepository extends JpaRepository<Hotel, Long> {
}
