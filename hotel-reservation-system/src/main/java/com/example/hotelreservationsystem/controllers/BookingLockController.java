package com.example.hotelreservationsystem.controllers;

import com.example.hotelreservationsystem.dto.BookingLockRequest;
import com.example.hotelreservationsystem.dto.BookingLockResponse;
import com.example.hotelreservationsystem.dto.LockStatusResponse;
import com.example.hotelreservationsystem.entity.Customer;
import com.example.hotelreservationsystem.service.AuthenticationService;
import com.example.hotelreservationsystem.service.BookingLockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings/lock")
@RequiredArgsConstructor
@Slf4j
public class BookingLockController {

    private final BookingLockService bookingLockService;
    private final AuthenticationService authenticationService;

    /**
     * Create a booking lock for a room
     * POST /api/bookings/lock
     * Requires authentication - customerId is extracted from JWT token
     *
     * @param request The lock request containing roomId
     * @return BookingLockResponse with lock details
     */
    @PostMapping
    public ResponseEntity<BookingLockResponse> createLock(@Valid @RequestBody BookingLockRequest request) {
        try {
            // Extract customer from JWT token
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            var email = authentication.getName();
            Customer customer = authenticationService.getCurrentUser(email);
            Long customerId = customer.getId();

            log.info("Creating lock for room {} by customer {}", request.getRoomId(), customerId);

            var lockId = bookingLockService.createLock(request.getRoomId(), customerId);

            // Calculate expiration time
            var ttlSeconds = bookingLockService.getLockTtl(request.getRoomId());
            var expiresAt = LocalDateTime.now().plusSeconds(ttlSeconds != null ? ttlSeconds : 600);

            var response = BookingLockResponse.builder()
                                              .lockId(lockId)
                                              .roomId(request.getRoomId())
                                              .customerId(customerId)
                                              .expiresAt(expiresAt)
                                              .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalStateException e) {
            log.warn("Room {} is already locked", request.getRoomId());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            log.error("Failed to create lock for room {}", request.getRoomId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Release a booking lock
     * DELETE /api/bookings/lock/{lockId}
     * Requires authentication - customerId is extracted from JWT token
     *
     * @param lockId The lock ID to release
     * @return 204 No Content on success, 401 Unauthorized if not owner, 404 if not found
     */
    @DeleteMapping("/{lockId}")
    public ResponseEntity<Void> releaseLock(@PathVariable String lockId) {
        try {
            // Extract customer from JWT token
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            var email = authentication.getName();
            var customer = authenticationService.getCurrentUser(email);
            var customerId = customer.getId();

            log.info("Releasing lock {} by customer {}", lockId, customerId);

            var released = bookingLockService.releaseLock(lockId, customerId);

            if (!released)
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            log.error("Failed to release lock {}", lockId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get lock status for a room
     * GET /api/bookings/lock/{roomId}/status
     *
     * @param roomId The room ID to check
     * @return Lock status information
     */
    @GetMapping("/{roomId}/status")
    public ResponseEntity<LockStatusResponse> getLockStatus(@PathVariable Long roomId) {
        try {
            log.debug("Checking lock status for room {}", roomId);

            var isLocked = bookingLockService.isLocked(roomId);

            if (!isLocked) {
                var response = LockStatusResponse.builder()
                                                 .isLocked(false)
                                                 .build();
                return ResponseEntity.ok(response);
            }

            // Get lock details
            Map<String, Object> lockInfo = bookingLockService.getLockInfo(roomId);

            if (lockInfo == null) {
                var response = LockStatusResponse.builder()
                                                 .isLocked(false)
                                                 .build();
                return ResponseEntity.ok(response);
            }

            var ttlSeconds = bookingLockService.getLockTtl(roomId);
            var expiresAt = ttlSeconds != null && ttlSeconds > 0
                ? LocalDateTime.now().plusSeconds(ttlSeconds)
                : null;

            var customerId = ((Number) lockInfo.get("customerId")).longValue();

            var response = LockStatusResponse.builder()
                                             .isLocked(true)
                                             .lockedBy(customerId)
                                             .expiresAt(expiresAt)
                                             .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get lock status for room {}", roomId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
