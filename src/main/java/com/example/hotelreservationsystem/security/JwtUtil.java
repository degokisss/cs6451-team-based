package com.example.hotelreservationsystem.security;

import com.example.hotelreservationsystem.service.TokenStorageService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final TokenStorageService tokenStorageService;

    @Value("${jwt.secret:gang-of-four-secret}")
    private String secret;

    @Value("${jwt.expiration:1800000}") // 30 mins
    private Long expiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final var claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                   .verifyWith(getSigningKey())
                   .build()
                   .parseSignedClaims(token)
                   .getPayload();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String generateToken(String email) {
        var claims = new HashMap<String, Object>();
        String token = createToken(claims, email);
        // Store token in Redis with TTL
        tokenStorageService.storeToken(email, token, expiration);
        return token;
    }

    public String generateToken(String email, Map<String, Object> extraClaims) {
        String token = createToken(extraClaims, email);
        // Store token in Redis with TTL
        tokenStorageService.storeToken(email, token, expiration);
        return token;
    }

    private String createToken(Map<String, Object> claims, String subject) {
        var now = new Date();
        var expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                   .claims(claims)
                   .subject(subject)
                   .issuedAt(now)
                   .expiration(expiryDate)
                   .signWith(getSigningKey(), Jwts.SIG.HS256)
                   .compact();
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final var email = extractEmail(token);
        return (email.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    public Boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
}
