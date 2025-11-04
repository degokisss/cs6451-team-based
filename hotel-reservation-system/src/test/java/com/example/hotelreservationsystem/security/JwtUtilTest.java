package com.example.hotelreservationsystem.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final String testEmail = "test@example.com";
    private final String testSecret = "gang-of-four-secret-gang-of-four-secret-";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", testSecret);
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86400000L);
    }

    @Test
    void testGenerateToken() {
        var token = jwtUtil.generateToken(testEmail);
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void testGenerateTokenWithClaims() {
        var claims = new HashMap<String, Object>();
        claims.put("role", "USER");
        claims.put("name", "Test User");

        var token = jwtUtil.generateToken(testEmail, claims);
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void testExtractEmail() {
        var token = jwtUtil.generateToken(testEmail);
        var extractedEmail = jwtUtil.extractEmail(token);
        assertEquals(testEmail, extractedEmail);
    }

    @Test
    void testValidateToken() {
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
        var token = jwtUtil.generateToken(testEmail);
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void testValidateInvalidToken() {
        assertFalse(jwtUtil.validateToken("invalid.token.here"));
    }

    @Test
    void testExtractExpiration() {
        var token = jwtUtil.generateToken(testEmail);
        assertNotNull(jwtUtil.extractExpiration(token));
        assertTrue(jwtUtil.extractExpiration(token).getTime() > System.currentTimeMillis());
    }
}
