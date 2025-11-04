package com.example.hotelreservationsystem.service;

import com.example.hotelreservationsystem.dto.LoginRequest;
import com.example.hotelreservationsystem.dto.LoginResponse;
import com.example.hotelreservationsystem.dto.RegisterRequest;
import com.example.hotelreservationsystem.entity.Customer;
import com.example.hotelreservationsystem.enums.MembershipTier;
import com.example.hotelreservationsystem.enums.Role;
import com.example.hotelreservationsystem.exception.EmailAlreadyExistsException;
import com.example.hotelreservationsystem.exception.InvalidCredentialsException;
import com.example.hotelreservationsystem.repository.CustomerRepository;
import com.example.hotelreservationsystem.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class AuthenticationService{

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (customerRepository.findByEmail(request.getEmail()).isPresent())
            throw new EmailAlreadyExistsException("Email already registered");

        var customer = Customer.builder()
                               .name(request.getName())
                               .email(request.getEmail())
                               .passwordHash(passwordEncoder.encode(request.getPassword()))
                               .phoneNumber(request.getPhoneNumber())
                               .role(Role.USER)
                               .membershipTier(MembershipTier.BRONZE)
                               .enabled(true)
                               .build();

        var savedCustomer = customerRepository.save(customer);
        var token = tokenGeneration(customer);

        return LoginResponse.builder()
                            .token(token)
                            .email(savedCustomer.getEmail())
                            .name(savedCustomer.getName())
                            .build();
    }

    public LoginResponse login(LoginRequest request) {
        try {
            var authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
            if (!authentication.isAuthenticated())
                throw new BadCredentialsException("Bad credentials");

            var customer = customerRepository.findByEmail(request.getEmail())
                                             .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            var token = tokenGeneration(customer);

            return LoginResponse.builder()
                                .token(token)
                                .email(customer.getEmail())
                                .name(customer.getName())
                                .build();
        } catch (BadCredentialsException e) {
            throw new InvalidCredentialsException("Invalid email or password", e);
        }
    }

    private String tokenGeneration(Customer customer) {
        var claims = new HashMap<String, Object>();
        claims.put("role", customer.getRole().name());
        claims.put("name", customer.getName());
        return String.format("Bearer %s", jwtUtil.generateToken(customer.getEmail(), claims));
    }

    public Customer getCurrentUser(String email) {
        return customerRepository.findByEmail(email)
                                 .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }
}
