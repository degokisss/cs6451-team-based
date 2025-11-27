package com.example.hotelreservationsystem.integration;

import com.example.hotelreservationsystem.dto.LoginRequest;
import com.example.hotelreservationsystem.dto.RegisterRequest;
import com.example.hotelreservationsystem.repository.CustomerRepository;
import com.example.hotelreservationsystem.service.TokenStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TokenValidationFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TokenStorageService tokenStorageService;

    @AfterEach
    void cleanup() {
        customerRepository.findByEmail("flowtest@example.com")
                          .ifPresent(customer -> customerRepository.delete(customer));
    }

    @Test
    void shouldCompleteFullAuthenticationFlow() throws Exception {
        var registerRequest = new RegisterRequest();
        registerRequest.setName("Flow Test User");
        registerRequest.setEmail("flowtest@example.com");
        registerRequest.setPassword("securePassword123");
        registerRequest.setPhoneNumber("1234567890");

        var registerResult = mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                                                                                  .content(objectMapper.writeValueAsString(registerRequest)))
                                    .andExpect(status().isOk())
                                    .andExpect(jsonPath("$.token").exists())
                                    .andReturn();

        var registerResponse = registerResult.getResponse().getContentAsString();
        var fullToken = objectMapper.readTree(registerResponse).get("token").asText();
        var token = fullToken.replace("Bearer ", "");

        assertTrue(tokenStorageService.validateToken(token), "Token should be in Redis after registration");

        mockMvc.perform(get("/api/auth/me").header("Authorization", fullToken))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.email").value("flowtest@example.com"));

        mockMvc.perform(post("/api/auth/logout").header("Authorization", fullToken))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.message").value("Logout successful"));

        assertFalse(tokenStorageService.validateToken(token), "Token should be removed from Redis after logout");

        mockMvc.perform(get("/api/auth/me").header("Authorization", fullToken))
               .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldStoreTokenInRedisOnLogin() throws Exception {
        var registerRequest = new RegisterRequest();
        registerRequest.setName("Login Test User");
        registerRequest.setEmail("flowtest@example.com");
        registerRequest.setPassword("securePassword123");
        registerRequest.setPhoneNumber("1234567890");

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                                                             .content(objectMapper.writeValueAsString(registerRequest)));

        var loginRequest = new LoginRequest();
        loginRequest.setEmail("flowtest@example.com");
        loginRequest.setPassword("securePassword123");

        var loginResult = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                                                                            .content(objectMapper.writeValueAsString(loginRequest)))
                                 .andExpect(status().isOk())
                                 .andExpect(jsonPath("$.token").exists())
                                 .andReturn();

        var loginResponse = loginResult.getResponse().getContentAsString();
        var fullToken = objectMapper.readTree(loginResponse).get("token").asText();
        var token = fullToken.replace("Bearer ", "");

        assertTrue(tokenStorageService.validateToken(token), "Token should be in Redis after login");

        mockMvc.perform(get("/api/auth/me").header("Authorization", fullToken))
               .andExpect(status().isOk());
    }

    @Test
    void shouldDenyAccessWithoutTokenInRedis() throws Exception {
        var fakeToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIn0.test";

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + fakeToken))
               .andExpect(status().isUnauthorized());
    }
}
