package com.example.hotelreservationsystem.controllers;

import com.example.hotelreservationsystem.dto.BookingLockRequest;
import com.example.hotelreservationsystem.entity.Customer;
import com.example.hotelreservationsystem.service.AuthenticationService;
import com.example.hotelreservationsystem.service.BookingLockService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BookingLockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BookingLockService bookingLockService;

    @MockitoBean
    private AuthenticationService authenticationService;

    private Customer mockCustomer;

    @BeforeEach
    void setUp() {
        // Mock authenticated user
        mockCustomer = new Customer();
        mockCustomer.setId(100L);
        mockCustomer.setEmail("user@test.com");
        mockCustomer.setName("Test User");

        // Mock authentication service to return the mock customer
        when(authenticationService.getCurrentUser(anyString())).thenReturn(mockCustomer);
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void shouldCreateLockSuccessfully() throws Exception {
        var request = BookingLockRequest.builder()
            .roomId(1L)
            .build();

        when(bookingLockService.createLock(1L, 100L)).thenReturn("test-lock-id");
        when(bookingLockService.getLockTtl(1L)).thenReturn(600L);

        mockMvc.perform(post("/api/bookings/lock")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.lockId").value("test-lock-id"))
            .andExpect(jsonPath("$.roomId").value(1))
            .andExpect(jsonPath("$.customerId").value(100))
            .andExpect(jsonPath("$.expiresAt").exists());
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void shouldReturn409WhenRoomAlreadyLocked() throws Exception {
        var request = BookingLockRequest.builder()
            .roomId(1L)
            .build();

        when(bookingLockService.createLock(1L, 100L))
            .thenThrow(new IllegalStateException("Room is already locked"));

        mockMvc.perform(post("/api/bookings/lock")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void shouldReleaseLockSuccessfully() throws Exception {
        when(bookingLockService.releaseLock("test-lock-id", 100L)).thenReturn(true);

        mockMvc.perform(delete("/api/bookings/lock/test-lock-id")
                .with(csrf()))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void shouldReturn401WhenReleasingOthersLock() throws Exception {
        when(bookingLockService.releaseLock("test-lock-id", 100L)).thenReturn(false);

        mockMvc.perform(delete("/api/bookings/lock/test-lock-id")
                .with(csrf()))
            .andExpect(status().isUnauthorized());
    }


    @Test
    @WithMockUser(username = "user@test.com")
    void shouldValidateRequestBody() throws Exception {
        var invalidRequest = "{}"; // Missing required roomId field

        mockMvc.perform(post("/api/bookings/lock")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
            .andExpect(status().isBadRequest());
    }
}
