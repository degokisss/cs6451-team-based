package com.example.hotelreservationsystem.controllers;

import com.example.hotelreservationsystem.dto.BookingCreateRequest;
import com.example.hotelreservationsystem.dto.BookingResponse;
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
     * Create a booking order from a lock
     * POST /api/bookings
     * Requires authentication - customerId is extracted from JWT token
     *
     * @param request The booking creation request
     * @return BookingResponse with order details
     */
    @PostMapping
    public ResponseEntity<?> createBooking(@Valid @RequestBody BookingCreateRequest request) {
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
}
