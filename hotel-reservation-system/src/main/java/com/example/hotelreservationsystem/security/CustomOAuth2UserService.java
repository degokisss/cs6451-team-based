package com.example.hotelreservationsystem.security;

import com.example.hotelreservationsystem.entity.Customer;
import com.example.hotelreservationsystem.enums.MembershipTier;
import com.example.hotelreservationsystem.enums.Role;
import com.example.hotelreservationsystem.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final CustomerRepository customerRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        var oauth2User = super.loadUser(userRequest);

        // Get provider information
        var provider = userRequest.getClientRegistration().getRegistrationId(); // "google"
        var providerId = Objects.requireNonNull(oauth2User.getAttribute("sub")).toString(); // Google's unique user ID
        var email = Objects.requireNonNull(oauth2User.getAttribute("email")).toString();
        var name = Objects.requireNonNull(oauth2User.getAttribute("name")).toString();

        // Try to find existing user by OAuth provider and ID
        var existingCustomer = customerRepository.findByOauthProviderAndOauthProviderId(provider, providerId);

        Customer customer;
        if (existingCustomer.isPresent()) {
            // Update existing customer
            customer = existingCustomer.get();
            customer.setName(name);
            customer.setEmail(email);
        } else {
            // Check if user with this email already exists (traditional registration)
            var customerByEmail = customerRepository.findByEmail(email);
            if (customerByEmail.isPresent()) {
                // Link OAuth account to existing user
                customer = customerByEmail.get();
                customer.setOauthProvider(provider);
                customer.setOauthProviderId(providerId);
            } else {
                // Create new customer
                customer = Customer.builder()
                                   .name(name)
                                   .email(email)
                                   .oauthProvider(provider)
                                   .oauthProviderId(providerId)
                                   .membershipTier(MembershipTier.BRONZE)
                                   .role(Role.USER)
                                   .enabled(true)
                                   .build();
            }
        }

        customerRepository.save(customer);

        // Return a custom OAuth2User that includes our Customer entity
        return new CustomOAuth2User(oauth2User, customer);
    }
}
