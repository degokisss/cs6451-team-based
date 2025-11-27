package com.example.hotelreservationsystem.controllers;

import com.example.hotelreservationsystem.service.TokenStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LogoutEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TokenStorageService tokenStorageService;

    @Test
    @WithMockUser
    void shouldLogoutSuccessfully() throws Exception {
        var token = "valid-jwt-token";
        when(tokenStorageService.removeToken(token)).thenReturn(true);

        mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.message").value("Logout successful"));

        verify(tokenStorageService).removeToken(token);
    }

    @Test
    @WithMockUser
    void shouldReturnOkWhenTokenNotFound() throws Exception {
        var token = "non-existent-token";
        when(tokenStorageService.removeToken(token)).thenReturn(false);

        mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.message").value("Token not found or already logged out"));
    }

    @Test
    @WithMockUser
    void shouldReturnBadRequestWhenAuthHeaderMissing() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.message").value("Invalid Authorization header"));
    }

    @Test
    @WithMockUser
    void shouldReturnBadRequestWhenAuthHeaderInvalid() throws Exception {
        mockMvc.perform(post("/api/auth/logout").header("Authorization", "Invalid header"))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.message").value("Invalid Authorization header"));
    }

    @Test
    @WithMockUser
    void shouldHandleEmptyBearerToken() throws Exception {
        when(tokenStorageService.removeToken("")).thenReturn(false);

        mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer "))
               .andExpect(status().isOk());
    }
}
