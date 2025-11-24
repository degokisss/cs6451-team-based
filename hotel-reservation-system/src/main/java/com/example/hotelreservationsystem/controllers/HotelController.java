package com.example.hotelreservationsystem.controllers;

import com.example.hotelreservationsystem.dto.*;
import com.example.hotelreservationsystem.entity.Hotel;
import com.example.hotelreservationsystem.entity.Order;
import com.example.hotelreservationsystem.service.HotelService;
import com.example.hotelreservationsystem.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/hotel")
public class HotelController {

    @Autowired
    HotelService hotelService;

    @Autowired
    RoomService roomService;

    @PostMapping
    public ResponseEntity<HotelCreateResponse> addHotel(@RequestBody HotelCreateRequest hotelCreateRequest) {
        var response = hotelService.create(hotelCreateRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{hotelId}/rooms")
    public ResponseEntity<RoomCreateResponse> addRoomToHotel(@PathVariable Long hotelId, @RequestBody @jakarta.validation.Valid RoomForHotelCreateRequest request) {
        // build a RoomCreateRequest using hotelId from path
        var roomReq = new RoomCreateRequest();
        roomReq.setHotelId(hotelId);
        roomReq.setRoomTypeId(request.getRoomTypeId());
        roomReq.setRoomNumber(request.getRoomNumber());
        roomReq.setRoomStatus(request.getRoomStatus());

        var response = roomService.create(roomReq);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/hotels/list")
    public List<Hotel> getHotels() {
        try {
            return hotelService.findAll();
        } catch (Exception e) {
            log.error(e.getMessage());
            return List.of();
        }
    }

    @GetMapping("/hotels/{id}")
    public ResponseEntity<Hotel> getHotelById(@PathVariable Long id) {
        try {
            var hotel = hotelService.findById(id);
            if (hotel == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(hotel);
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/hotels/{id}")
    public ResponseEntity<HotelCreateResponse> updateHotel(@PathVariable Long id, @RequestBody HotelCreateRequest hotelCreateRequest) {
        try {
            var response = hotelService.update(id, hotelCreateRequest);
            if (response == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw e;
        }
    }

    @DeleteMapping("/hotels/{id}")
    public ResponseEntity<Void> deleteHotel(@PathVariable Long id) {
        try {
            var deleted = hotelService.delete(id);
            if (!deleted) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("checkin")
    public ResponseEntity<CheckInResponse> checkIn(@RequestBody CheckInRequest checkInRequest) {
        try {
            CheckInResponse response = hotelService.checkIn(checkInRequest.orderId(), checkInRequest.checkInCode());
            if (response == null) {
                return ResponseEntity.badRequest().build();
            } else {
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }


    @PostMapping("checkout")
    public ResponseEntity<CheckoutResponse> checkOut(@RequestBody CheckoutRequest request) {
        try {
            CheckoutResponse response = hotelService.checkOut(request.roomId());
            if (response == null) {
                return ResponseEntity.badRequest().build();
            } else {
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("orders")
    public ResponseEntity<List<Order>> getAllOrders() {
        try {
            List<Order> orders = hotelService.getAllOrders();
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.internalServerError().build();
        }

    }
}