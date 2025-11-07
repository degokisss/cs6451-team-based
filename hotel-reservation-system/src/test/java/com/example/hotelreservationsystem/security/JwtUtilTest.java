package com.example.hotelreservationsystem.security;

import com.example.hotelreservationsystem.service.TokenStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doNothing;

@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    @Mock
    private TokenStorageService tokenStorageService;

    private JwtUtil jwtUtil;
    private final String testEmail = "test@example.com";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(tokenStorageService);
        var testSecret = "gang-of-four-secret-gang-of-four-secret";
        ReflectionTestUtils.setField(jwtUtil, "secret", testSecret);
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86400000L);
    }

    @Test
    void testGenerateToken() {
        doNothing().when(tokenStorageService).storeToken(anyString(), anyString(), anyLong());

        var token = jwtUtil.generateToken(testEmail);
        assertNotNull(token);
        assertFalse(token.isEmpty());

        verify(tokenStorageService, times(1)).storeToken(eq(testEmail), anyString(), eq(86400000L));
    }

    @Test
    void testGenerateTokenWithClaims() {
        doNothing().when(tokenStorageService).storeToken(anyString(), anyString(), anyLong());

        var claims = new HashMap<String, Object>();
        claims.put("role", "USER");
        claims.put("name", "Test User");

        var token = jwtUtil.generateToken(testEmail, claims);
        assertNotNull(token);
        assertFalse(token.isEmpty());

        verify(tokenStorageService, times(1)).storeToken(eq(testEmail), anyString(), eq(86400000L));
    }

    @Test
    void testExtractEmail() {
        doNothing().when(tokenStorageService).storeToken(anyString(), anyString(), anyLong());
        var token = jwtUtil.generateToken(testEmail);
        var extractedEmail = jwtUtil.extractEmail(token);
        assertEquals(testEmail, extractedEmail);
    }

    @Test
    void testValidateToken() {
        doNothing().when(tokenStorageService).storeToken(anyString(), anyString(), anyLong());
        var token = jwtUtil.generateToken(testEmail);
        var userDetails = User.builder()
                              .username(testEmail)
                              .password("password")
                              .authorities(new ArrayList<>())
                              .build();

        assertTrue(jwtUtil.validateToken(token, userDetails));
    }

    @Test
    void testValidateTokenWithWrongUser() {
        doNothing().when(tokenStorageService).storeToken(anyString(), anyString(), anyLong());
        var token = jwtUtil.generateToken(testEmail);
        var userDetails = User.builder()
                              .username("wrong@example.com")
                              .password("password")
                              .authorities(new ArrayList<>())
                              .build();

        assertFalse(jwtUtil.validateToken(token, userDetails));
    }

    @Test
    void testValidateTokenSimple() {
        doNothing().when(tokenStorageService).storeToken(anyString(), anyString(), anyLong());
        var token = jwtUtil.generateToken(testEmail);
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void testValidateInvalidToken() {
        assertFalse(jwtUtil.validateToken("invalid.token.here"));
    }

    @Test
    void testExtractExpiration() {
        doNothing().when(tokenStorageService).storeToken(anyString(), anyString(), anyLong());
        var token = jwtUtil.generateToken(testEmail);
        assertNotNull(jwtUtil.extractExpiration(token));
        assertTrue(jwtUtil.extractExpiration(token).getTime() > System.currentTimeMillis());
    }
}
