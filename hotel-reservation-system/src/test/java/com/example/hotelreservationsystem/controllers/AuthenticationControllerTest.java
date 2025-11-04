package com.example.hotelreservationsystem.controllers;

import com.example.hotelreservationsystem.dto.LoginRequest;
import com.example.hotelreservationsystem.dto.LoginResponse;
import com.example.hotelreservationsystem.dto.RegisterRequest;
import com.example.hotelreservationsystem.entity.Customer;
import com.example.hotelreservationsystem.enums.MembershipTier;
import com.example.hotelreservationsystem.enums.Role;
import com.example.hotelreservationsystem.exception.EmailAlreadyExistsException;
import com.example.hotelreservationsystem.exception.InvalidCredentialsException;
import com.example.hotelreservationsystem.service.AuthenticationService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticationService authenticationService;

    private LoginResponse mockLoginResponse;
    private Customer mockCustomer;

    @BeforeEach
    void setUp() {
        mockLoginResponse = LoginResponse.builder()
                                         .token("mock.jwt.token")
                                         .type("Bearer")
                                         .email("test@example.com")
                                         .name("Test User")
                                         .build();

        mockCustomer = new Customer();
        mockCustomer.setId(1L);
        mockCustomer.setName("Test User");
        mockCustomer.setEmail("test@example.com");
        mockCustomer.setPhoneNumber("1234567890");
        mockCustomer.setMembershipTier(MembershipTier.BRONZE);
        mockCustomer.setRole(Role.USER);
    }

    @Test
    void testRegister_Success() throws Exception {
        var request = new RegisterRequest("Test User", "test@example.com", "password123", "1234567890");

        when(authenticationService.register(any(RegisterRequest.class))).thenReturn(mockLoginResponse);

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                                                  .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.token").value("mock.jwt.token"))
               .andExpect(jsonPath("$.email").value("test@example.com"))
               .andExpect(jsonPath("$.name").value("Test User"));
    }

    @Test
    void testRegister_EmailAlreadyExists() throws Exception {
        var request = new RegisterRequest("Test User", "test@example.com", "password123", "1234567890");

        when(authenticationService.register(any(RegisterRequest.class))).thenThrow(new EmailAlreadyExistsException("Email already registered"));

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                                                  .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.error").value("Registration Failed"))
               .andExpect(jsonPath("$.message").value("Email already registered"))
               .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void testLogin_Success() throws Exception {
        var request = new LoginRequest("test@example.com", "password123");

        when(authenticationService.login(any(LoginRequest.class))).thenReturn(mockLoginResponse);

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                                               .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.token").value("mock.jwt.token"))
               .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void testLogin_InvalidCredentials() throws Exception {
        var request = new LoginRequest("test@example.com", "wrongpassword");

        when(authenticationService.login(any(LoginRequest.class))).thenThrow(new InvalidCredentialsException("Invalid email or password"));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                                               .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isUnauthorized())
               .andExpect(jsonPath("$.error").value("Authentication Failed"))
               .andExpect(jsonPath("$.message").value("Invalid email or password"))
               .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void testGetCurrentUser_Success() throws Exception {
        when(authenticationService.getCurrentUser("test@example.com")).thenReturn(mockCustomer);

        mockMvc.perform(get("/api/auth/me"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.email").value("test@example.com"))
               .andExpect(jsonPath("$.name").value("Test User"))
               .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void testHealthCheck() throws Exception {
        mockMvc.perform(get("/api/auth/health"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.status").value("Authentication service is running"));
    }
}
