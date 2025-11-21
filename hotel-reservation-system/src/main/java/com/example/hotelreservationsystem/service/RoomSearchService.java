package com.example.hotelreservationsystem.service;

import com.example.hotelreservationsystem.entity.Room;
import com.example.hotelreservationsystem.enums.RoomStatus;
import com.example.hotelreservationsystem.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomSearchService {

    private final RoomRepository roomRepository;
    private final BookingLockService bookingLockService;

    /**
     * Search for available rooms based on criteria
     * Excludes rooms that are locked or not available
     *
     * @param checkInDate  Check-in date (future enhancement for date-based booking)
     * @param checkOutDate Check-out date (future enhancement for date-based booking)
     * @param roomTypeId   Room type filter (optional)
     * @param hotelId      Hotel filter (optional)
     * @return List of available rooms
     */
    public List<Room> searchAvailableRooms(
        LocalDate checkInDate,
        LocalDate checkOutDate,
        Long roomTypeId,
        Long hotelId
    ) {
        log.info("Searching rooms - checkIn: {}, checkOut: {}, roomTypeId: {}, hotelId: {}",
            checkInDate, checkOutDate, roomTypeId, hotelId);

        List<Room> rooms;

        // Query based on provided filters
        if (roomTypeId != null) {
            // Filter by room type and available status
            rooms = roomRepository.findByRoomType_IdAndRoomStatus(roomTypeId, RoomStatus.AVAILABLE);
        } else {
            // Get all available rooms
            rooms = roomRepository.findAll().stream()
                                            .filter(room -> room.getRoomStatus() == RoomStatus.AVAILABLE)
                                            .collect(Collectors.toList());
        }

        // Filter by hotel if specified
        if (hotelId != null) {
            rooms = rooms.stream()
                         .filter(room -> room.getHotel() != null && room.getHotel().getId().equals(hotelId))
                         .collect(Collectors.toList());
        }

        // Exclude locked rooms
        rooms = rooms.stream()
                     .filter(room -> !bookingLockService.isLocked(room.getId()))
                     .collect(Collectors.toList());

        log.info("Found {} available rooms", rooms.size());
        return rooms;
    }

    /**
     * Get all available rooms (without filters)
     *
     * @return List of available, unlocked rooms
     */
    public List<Room> getAllAvailableRooms() {
        return searchAvailableRooms(null, null, null, null);
    }
}
