package com.example.hotelreservationsystem.service;

import com.example.hotelreservationsystem.dto.LoginRequest;
import com.example.hotelreservationsystem.dto.RegisterRequest;
import com.example.hotelreservationsystem.entity.Customer;
import com.example.hotelreservationsystem.enums.MembershipTier;
import com.example.hotelreservationsystem.enums.Role;
import com.example.hotelreservationsystem.exception.EmailAlreadyExistsException;
import com.example.hotelreservationsystem.exception.InvalidCredentialsException;
import com.example.hotelreservationsystem.repository.CustomerRepository;
import com.example.hotelreservationsystem.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthenticationService authenticationService;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    private Customer mockCustomer;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        mockCustomer = new Customer();
        mockCustomer.setId(1L);
        mockCustomer.setName("Test User");
        mockCustomer.setEmail("test@example.com");
        mockCustomer.setPasswordHash("hashedPassword");
        mockCustomer.setPhoneNumber("1234567890");
        mockCustomer.setRole(Role.USER);
        mockCustomer.setMembershipTier(MembershipTier.BRONZE);
        mockCustomer.setEnabled(true);

        registerRequest = new RegisterRequest("Test User", "test@example.com", "password123", "1234567890");
        loginRequest = new LoginRequest("test@example.com", "password123");
    }

    @Test
    void testLoadUserByUsername_Success() {
        when(customerRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockCustomer));

        var userDetails = userDetailsService.loadUserByUsername("test@example.com");

        assertNotNull(userDetails);
        assertEquals("test@example.com", userDetails.getUsername());
        verify(customerRepository, times(1)).findByEmail("test@example.com");
    }

    @Test
    void testLoadUserByUsername_NotFound() {
        when(customerRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername("test@example.com"));
    }

    @Test
    void testRegister_Success() {
        when(customerRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(customerRepository.save(any(Customer.class))).thenReturn(mockCustomer);
        when(jwtUtil.generateToken(anyString(), any(Map.class))).thenReturn("mock.jwt.token");

        var response = authenticationService.register(registerRequest);

        assertNotNull(response);
        assertEquals("Bearer mock.jwt.token", response.token());  // Token is wrapped with "Bearer " prefix
        assertEquals("test@example.com", response.email());
        assertEquals("Test User", response.name());
        verify(customerRepository, times(1)).save(any(Customer.class));
        verify(jwtUtil, times(1)).generateToken(anyString(), any(Map.class));
    }

    @Test
    void testRegister_EmailAlreadyExists() {
        when(customerRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockCustomer));

        assertThrows(EmailAlreadyExistsException.class, () -> authenticationService.register(registerRequest));

        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void testLogin_Success() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(customerRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockCustomer));
        when(jwtUtil.generateToken(anyString(), any(Map.class))).thenReturn("mock.jwt.token");

        var response = authenticationService.login(loginRequest);

        assertNotNull(response);
        assertEquals("Bearer mock.jwt.token", response.token());  // Token is wrapped with "Bearer " prefix
        assertEquals("test@example.com", response.email());
        assertEquals("Test User", response.name());
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void testLogin_InvalidCredentials() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenThrow(new BadCredentialsException("Invalid credentials"));

        assertThrows(InvalidCredentialsException.class, () -> authenticationService.login(loginRequest));
    }

    @Test
    void testLogin_UserNotFound() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);

        assertThrows(InvalidCredentialsException.class, () -> authenticationService.login(loginRequest));
    }

    @Test
    void testGetCurrentUser_Success() {
        when(customerRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockCustomer));

        var customer = authenticationService.getCurrentUser("test@example.com");

        assertNotNull(customer);
        assertEquals("test@example.com", customer.getEmail());
        assertEquals("Test User", customer.getName());
    }

    @Test
    void testGetCurrentUser_NotFound() {
        when(customerRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> authenticationService.getCurrentUser("test@example.com"));
    }
}
