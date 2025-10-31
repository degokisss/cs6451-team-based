package com.example.hotelreservationsystem.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "customer")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long customerID;

    private String name;
    private String email;
    private String phoneNumber;
    private MembershipTier membershipTier;
    private String token;
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
    public String getToken() {
        return token;
    }
    public void setToken(String token) {
        this.token = token;
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
