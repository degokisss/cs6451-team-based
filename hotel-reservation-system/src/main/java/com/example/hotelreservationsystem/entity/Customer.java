package com.example.hotelreservationsystem.entity;

import com.example.hotelreservationsystem.enums.MembershipTier;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "customer")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Customer extends BaseEntityAudit {
    @Column
    private String name;
    @Column
    private String email;
    @Column(name = "phone_number")
    private String phoneNumber;
    @Column(name = "password_hash")
    private String passwordHash;
    @Column(name = "membership_tier")
    @Enumerated(EnumType.STRING)
    private MembershipTier membershipTier;
    @Column(name = "google_auth_token")
    private String googleAuthToken;
}
