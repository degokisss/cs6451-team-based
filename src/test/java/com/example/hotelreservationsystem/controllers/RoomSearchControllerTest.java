package com.example.hotelreservationsystem.controllers;

import com.example.hotelreservationsystem.entity.Room;
import com.example.hotelreservationsystem.enums.RoomStatus;
import com.example.hotelreservationsystem.service.RoomSearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RoomSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoomSearchService roomSearchService;

    @Test
    @WithMockUser
    void shouldSearchRoomsSuccessfully() throws Exception {
        var room1 = Room.builder()
            .roomNumber("101")
            .roomStatus(RoomStatus.AVAILABLE)
            .build();
        room1.setId(1L);

        var room2 = Room.builder()
            .roomNumber("102")
            .roomStatus(RoomStatus.AVAILABLE)
            .build();
        room2.setId(2L);

        when(roomSearchService.searchAvailableRooms(any(), any(), any(), any()))
            .thenReturn(List.of(room1, room2));

        mockMvc.perform(get("/api/customer/rooms/search")
                .param("roomTypeId", "1")
                .param("hotelId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].roomNumber").value("101"))
            .andExpect(jsonPath("$[1].roomNumber").value("102"));
    }

    @Test
    @WithMockUser
    void shouldSearchRoomsWithDateParameters() throws Exception {
        when(roomSearchService.searchAvailableRooms(any(), any(), any(), any()))
            .thenReturn(List.of());

        mockMvc.perform(get("/api/customer/rooms/search")
                .param("checkInDate", "2025-12-01")
                .param("checkOutDate", "2025-12-05")
                .param("roomTypeId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser
    void shouldHandleSearchWithNoParameters() throws Exception {
        when(roomSearchService.searchAvailableRooms(any(), any(), any(), any()))
            .thenReturn(List.of());

        mockMvc.perform(get("/api/customer/rooms/search"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void shouldReturnEmptyListWhenNoRoomsAvailable() throws Exception {
        when(roomSearchService.searchAvailableRooms(any(), any(), any(), any()))
            .thenReturn(List.of());

        mockMvc.perform(get("/api/customer/rooms/search")
                .param("roomTypeId", "999"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }
}
