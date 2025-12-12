package com.example.hotelreservationsystem.controllers;

import com.example.hotelreservationsystem.dto.BookingCreateRequest;
import com.example.hotelreservationsystem.dto.BookingResponse;
import com.example.hotelreservationsystem.dto.CancellationRequest;
import com.example.hotelreservationsystem.dto.CancellationResponse;
import com.example.hotelreservationsystem.entity.Customer;
import com.example.hotelreservationsystem.service.AuthenticationService;
import com.example.hotelreservationsystem.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for managing booking orders
 */
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Slf4j
public class BookingController {

    private final BookingService bookingService;
    private final AuthenticationService authenticationService;

    /**
     * Get a booking order for the authenticated customer
     * GET /api/bookings/{orderId}
     *
     * @param orderId The order ID to retrieve
     * @return BookingResponse with order details
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<Object> getBooking(@PathVariable Long orderId) {
        try {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                log.warn("Unauthenticated access attempt for booking {}", orderId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
            }
            var email = authentication.getName();
            Customer customer = authenticationService.getCurrentUser(email);
            Long authenticatedCustomerId = customer.getId();

            BookingResponse response = bookingService.getBooking(orderId, authenticatedCustomerId);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Order not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (SecurityException e) {
            log.warn("Unauthorized booking access: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to get booking {}", orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to get booking");
        }
    }

    /**
     * Get a booking order by room for the authenticated customer
     * GET /api/bookings/room/{roomId}
     *
     * @param roomId The room ID to retrieve
     * @return BookingResponse with order details
     */
    @GetMapping("/room/{roomId}")
    public ResponseEntity<Object> getBookingByRoom(@PathVariable Long roomId) {
        try {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                log.warn("Unauthenticated access attempt for room {}", roomId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
            }
            var email = authentication.getName();
            Customer customer = authenticationService.getCurrentUser(email);
            Long authenticatedCustomerId = customer.getId();

            BookingResponse response = bookingService.getBookingByRoom(roomId, authenticatedCustomerId);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Order not found for room {}: {}", roomId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (SecurityException e) {
            log.warn("Unauthorized booking access for room {}: {}", roomId, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to get booking for room {}", roomId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to get booking");
        }
    }

    /**
     * Create a booking order from a lock
     * POST /api/bookings
     * Requires authentication - customerId is extracted from JWT token
     *
     * @param request The booking creation request
     * @return BookingResponse with order details
     */
    @PostMapping
    public ResponseEntity<Object> createBooking(@Valid @RequestBody BookingCreateRequest request) {
        try {
            // Extract customer from JWT token
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            var email = authentication.getName();
            Customer customer = authenticationService.getCurrentUser(email);
            Long authenticatedCustomerId = customer.getId();

            // Verify the request customerId matches the authenticated user
            if (!authenticatedCustomerId.equals(request.getCustomerId())) {
                log.warn("Customer ID mismatch: authenticated={}, requested={}",
                    authenticatedCustomerId, request.getCustomerId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Customer ID does not match authenticated user");
            }

            log.info("Creating booking from lock {} for customer {}", request.getLockId(), authenticatedCustomerId);

            BookingResponse response = bookingService.createBooking(request);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            // Invalid or expired lock
            log.warn("Invalid booking request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                 .body(e.getMessage());

        } catch (SecurityException e) {
            // Lock belongs to another customer
            log.warn("Unauthorized lock access: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                 .body(e.getMessage());

        } catch (IllegalStateException e) {
            // Room or customer not found
            log.error("Invalid state for booking creation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                 .body(e.getMessage());

        } catch (Exception e) {
            log.error("Failed to create booking", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Failed to create booking");
        }
    }

    /**
     * Cancel a booking order
     * DELETE /api/bookings/{orderId}
     * Requires authentication - customerId is extracted from JWT token
     *
     * @param orderId The order ID to cancel
     * @param request Optional cancellation request with reason
     * @return CancellationResponse with cancellation details
     */
    @DeleteMapping("/{orderId}")
    public ResponseEntity<Object> cancelBooking(
        @PathVariable Long orderId,
        @RequestBody(required = false) @Valid CancellationRequest request) {

        try {
            // Extract customer from JWT token
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            var email = authentication.getName();
            Customer customer = authenticationService.getCurrentUser(email);
            Long authenticatedCustomerId = customer.getId();

            log.info("Cancelling booking {} for customer {}", orderId, authenticatedCustomerId);

            // Extract cancellation reason if provided
            String cancellationReason = (request != null && request.getReason() != null)
                ? request.getReason()
                : null;

            CancellationResponse response = bookingService.cancelBooking(orderId, authenticatedCustomerId, cancellationReason);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            // Order not found
            log.warn("Order not found for cancellation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body(e.getMessage());

        } catch (SecurityException e) {
            // Unauthorized - customer doesn't own the order
            log.warn("Unauthorized cancellation attempt: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                 .body(e.getMessage());

        } catch (IllegalStateException e) {
            // Invalid status - order is COMPLETED or already CANCELLED
            log.warn("Invalid order status for cancellation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                 .body(e.getMessage());

        } catch (Exception e) {
            log.error("Failed to cancel booking", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Failed to cancel booking");
        }
    }
}
