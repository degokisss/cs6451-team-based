package com.example.hotelreservationsystem.dto;

import com.example.hotelreservationsystem.entity.Customer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

public record CustomOAuth2User(OAuth2User oauth2User, Customer customer) implements OAuth2User {

    @Override
    public Map<String, Object> getAttributes() {
        return oauth2User.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return customer.getAuthorities();
    }

    @Override
    public String getName() {
        return customer.getEmail();
    }
}
