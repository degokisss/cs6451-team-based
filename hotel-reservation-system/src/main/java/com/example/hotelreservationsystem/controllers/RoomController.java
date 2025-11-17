package com.example.hotelreservationsystem.controllers;

import com.example.hotelreservationsystem.dto.RoomCreateRequest;
import com.example.hotelreservationsystem.dto.RoomCreateResponse;
import com.example.hotelreservationsystem.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    @GetMapping("/search")
    public ResponseEntity<List<RoomCreateResponse>> searchRooms(@RequestParam Long roomTypeId){
        try {
            var rooms = roomService.searchedRooms(roomTypeId);
            var dtos = rooms.stream().map(room -> {
                Long hotelId = null;
                if (room.getHotel() != null) {
                    try { hotelId = room.getHotel().getId(); } catch (Exception ignored) {}
                }

                Long roomTypeIdLocal = null;
                if (room.getRoomType() != null) {
                    try { roomTypeIdLocal = room.getRoomType().getId(); } catch (Exception ignored) {}
                }

                return RoomCreateResponse.builder()
                        .id(room.getId())
                        .roomNumber(room.getRoomNumber())
                        .hotelId(hotelId)
                        .roomTypeId(roomTypeIdLocal)
                        .roomStatus(room.getRoomStatus())
                        .build();
            }).toList();

            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }


    @GetMapping("/{id:\\d+}")
    public ResponseEntity<com.example.hotelreservationsystem.dto.RoomCreateResponse> getRoomById(@PathVariable Long id) {
        try {
            var dto = roomService.findDtoById(id);
            if (dto == null) return ResponseEntity.notFound().build();
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping({"/list", ""})
    public ResponseEntity<List<RoomCreateResponse>> listRooms() {
        try {
            var dtos = roomService.findAllDto();
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }


    @PostMapping
    public ResponseEntity<RoomCreateResponse> addRoom(@RequestBody @Valid RoomCreateRequest request) {
        try {
            var response = roomService.create(request);
            if (response == null) return ResponseEntity.notFound().build();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw e;
        }
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<RoomCreateResponse> updateRoom(@PathVariable Long id, @RequestBody @Valid RoomCreateRequest request) {
        try {
            var response = roomService.update(id, request);
            if (response == null) return ResponseEntity.notFound().build();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw e;
        }
    }

    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id) {
        try {
            var deleted = roomService.delete(id);
            if (!deleted) return ResponseEntity.notFound().build();
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

}
