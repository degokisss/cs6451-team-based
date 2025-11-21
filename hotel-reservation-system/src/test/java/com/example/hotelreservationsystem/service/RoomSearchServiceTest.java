package com.example.hotelreservationsystem.service;

import com.example.hotelreservationsystem.entity.Hotel;
import com.example.hotelreservationsystem.entity.Room;
import com.example.hotelreservationsystem.entity.RoomType;
import com.example.hotelreservationsystem.enums.RoomStatus;
import com.example.hotelreservationsystem.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomSearchServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private BookingLockService bookingLockService;

    @InjectMocks
    private RoomSearchService roomSearchService;

    private Room room1;
    private Room room2;
    private Room room3;
    private Hotel hotel1;
    private Hotel hotel2;
    private RoomType roomType1;

    @BeforeEach
    void setUp() {
        // Create test hotels
        hotel1 = new Hotel();
        hotel1.setId(1L);

        hotel2 = new Hotel();
        hotel2.setId(2L);

        // Create test room type
        roomType1 = new RoomType();
        roomType1.setId(1L);

        // Create test rooms
        room1 = Room.builder()
            .roomNumber("101")
            .roomStatus(RoomStatus.AVAILABLE)
            .hotel(hotel1)
            .roomType(roomType1)
            .build();
        room1.setId(1L);

        room2 = Room.builder()
            .roomNumber("102")
            .roomStatus(RoomStatus.AVAILABLE)
            .hotel(hotel1)
            .roomType(roomType1)
            .build();
        room2.setId(2L);

        room3 = Room.builder()
            .roomNumber("201")
            .roomStatus(RoomStatus.AVAILABLE)
            .hotel(hotel2)
            .roomType(roomType1)
            .build();
        room3.setId(3L);
    }

    @Test
    void shouldReturnAvailableRoomsFilteredByRoomType() {
        when(roomRepository.findByRoomType_IdAndRoomStatus(1L, RoomStatus.AVAILABLE))
            .thenReturn(List.of(room1, room2, room3));
        when(bookingLockService.isLocked(anyLong())).thenReturn(false);

        var results = roomSearchService.searchAvailableRooms(null, null, 1L, null);

        assertEquals(3, results.size());
        verify(roomRepository).findByRoomType_IdAndRoomStatus(1L, RoomStatus.AVAILABLE);
    }

    @Test
    void shouldExcludeLockedRooms() {
        when(roomRepository.findByRoomType_IdAndRoomStatus(1L, RoomStatus.AVAILABLE))
            .thenReturn(List.of(room1, room2, room3));
        when(bookingLockService.isLocked(1L)).thenReturn(true);  // room1 is locked
        when(bookingLockService.isLocked(2L)).thenReturn(false);
        when(bookingLockService.isLocked(3L)).thenReturn(false);

        var results = roomSearchService.searchAvailableRooms(null, null, 1L, null);

        assertEquals(2, results.size());
        assertEquals("102", results.get(0).getRoomNumber());
        assertEquals("201", results.get(1).getRoomNumber());
    }

    @Test
    void shouldFilterByHotel() {
        when(roomRepository.findByRoomType_IdAndRoomStatus(1L, RoomStatus.AVAILABLE))
            .thenReturn(List.of(room1, room2, room3));
        when(bookingLockService.isLocked(anyLong())).thenReturn(false);

        var results = roomSearchService.searchAvailableRooms(null, null, 1L, 1L);

        assertEquals(2, results.size());
        assertEquals(1L, results.get(0).getHotel().getId());
        assertEquals(1L, results.get(1).getHotel().getId());
    }

    @Test
    void shouldReturnAllAvailableRoomsWhenNoFilters() {
        when(roomRepository.findAll()).thenReturn(List.of(room1, room2, room3));
        when(bookingLockService.isLocked(anyLong())).thenReturn(false);

        var results = roomSearchService.searchAvailableRooms(null, null, null, null);

        assertEquals(3, results.size());
        verify(roomRepository).findAll();
    }

    @Test
    void shouldReturnEmptyListWhenAllRoomsLocked() {
        when(roomRepository.findByRoomType_IdAndRoomStatus(1L, RoomStatus.AVAILABLE))
            .thenReturn(List.of(room1, room2, room3));
        when(bookingLockService.isLocked(anyLong())).thenReturn(true);

        var results = roomSearchService.searchAvailableRooms(null, null, 1L, null);

        assertEquals(0, results.size());
    }

    @Test
    void shouldHandleCheckInCheckOutDatesGracefully() {
        // Dates are currently not used in filtering but are parameters for future enhancement
        var checkIn = LocalDate.now().plusDays(1);
        var checkOut = LocalDate.now().plusDays(3);

        when(roomRepository.findByRoomType_IdAndRoomStatus(1L, RoomStatus.AVAILABLE))
            .thenReturn(List.of(room1, room2));
        when(bookingLockService.isLocked(anyLong())).thenReturn(false);

        var results = roomSearchService.searchAvailableRooms(checkIn, checkOut, 1L, null);

        assertEquals(2, results.size());
    }
}
