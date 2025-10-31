package com.example.hotelreservationsystem.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@Entity
@Table(name = "customer")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long customerID;

    private String name;
    private String email;
    private String phoneNumber;
    private String passwordHash;
    private MembershipTier membershipTier;
    private String googleAuthToken;


    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    public MembershipTier getMembershipTier() {
        return membershipTier;
    }
    public void setMembershipTier(MembershipTier membershipTier) {
        this.membershipTier = membershipTier;
    }
    public String getPasswordHash() {
        return passwordHash;
    }
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
    public String getGoogleAuthToken() {
        return googleAuthToken;
    }
    public void setGoogleAuthToken(String googleAuthToken) {
        this.googleAuthToken = googleAuthToken;
    }
    public Long getCustomerID() {
        return customerID;
    }
}
