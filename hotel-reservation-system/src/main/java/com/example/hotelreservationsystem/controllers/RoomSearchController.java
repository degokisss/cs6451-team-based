package com.example.hotelreservationsystem.controllers;

import com.example.hotelreservationsystem.entity.Room;
import com.example.hotelreservationsystem.service.RoomSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/customer/rooms")
@RequiredArgsConstructor
@Slf4j
public class RoomSearchController {

    private final RoomSearchService roomSearchService;

    /**
     * Search for available rooms with observer pattern support
     * GET /api/customer/rooms/search
     *
     * @param checkInDate  Check-in date (optional, for future date-based booking)
     * @param checkOutDate Check-out date (optional, for future date-based booking)
     * @param roomTypeId   Room type filter (optional)
     * @param hotelId      Hotel filter (optional)
     * @param customerId   Customer ID for tracking and notifications (optional)
     * @return List of available rooms excluding locked rooms
     * <p>
     * Note: Including customerId enables:
     * - Notification when locked rooms matching search criteria become available
     * - Analytics on search-to-booking conversion
     * - Personalized search experience
     */
    @GetMapping("/search")
    public ResponseEntity<List<Room>> searchRooms(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutDate,
        @RequestParam(required = false) Long roomTypeId,
        @RequestParam(required = false) Long hotelId,
        @RequestParam(required = false) Long customerId
    ) {
        try {
            log.info("Room search request - checkIn: {}, checkOut: {}, roomTypeId: {}, hotelId: {}, customerId: {}",
                checkInDate, checkOutDate, roomTypeId, hotelId, customerId);

            // Pass customerId to service for observer pattern tracking
            var rooms = roomSearchService.searchAvailableRooms(
                checkInDate, checkOutDate, roomTypeId, hotelId, customerId
            );

            return ResponseEntity.ok(rooms);

        } catch (Exception e) {
            log.error("Failed to search rooms", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
