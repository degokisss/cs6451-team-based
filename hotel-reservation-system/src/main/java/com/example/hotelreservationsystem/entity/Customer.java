package com.example.hotelreservationsystem.entity;

import com.example.hotelreservationsystem.enums.MembershipTier;
import com.example.hotelreservationsystem.enums.Role;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "customer", indexes = {
    @Index(name = "idx_customer_email", columnList = "email", unique = true)
})
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class Customer extends BaseEntityAudit implements UserDetails {
    @Column
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "password_hash")
    @JsonIgnore
    private String passwordHash;

    @Column(name = "membership_tier")
    @Enumerated(EnumType.STRING)
    private MembershipTier membershipTier;

    @Column(name = "google_auth_token")
    private String googleAuthToken;

    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private Role role = Role.USER;

    @Column(name = "enabled")
    private boolean enabled = true;

    // UserDetails implementation
    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    @JsonIgnore
    public String getPassword() {
        return passwordHash;
    }

    @Override
    @JsonIgnore
    public String getUsername() {
        return email;
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isEnabled() {
        return enabled;
    }
}
