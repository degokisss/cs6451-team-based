package com.example.hotelreservationsystem.security;

import com.example.hotelreservationsystem.dto.CustomOAuth2User;
import com.example.hotelreservationsystem.entity.Customer;
import com.example.hotelreservationsystem.enums.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.RedirectStrategy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationSuccessHandlerTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Authentication authentication;

    @Mock
    private RedirectStrategy redirectStrategy;

    @InjectMocks
    private OAuth2AuthenticationSuccessHandler successHandler;

    private Customer testCustomer;
    private CustomOAuth2User customOAuth2User;

    @BeforeEach
    void setUp() {
        // Create test customer
        testCustomer = Customer.builder()
                               .email("test@example.com")
                               .name("Test User")
                               .oauthProvider("google")
                               .oauthProviderId("google-123")
                               .role(Role.USER)
                               .enabled(true)
                               .build();
        testCustomer.setId(1L);

        // Create OAuth2User attributes
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", "google-123");
        attributes.put("email", "test@example.com");
        attributes.put("name", "Test User");

        var oauth2User = new DefaultOAuth2User(List.of(), attributes, "sub");

        // Create CustomOAuth2User
        customOAuth2User = new CustomOAuth2User(oauth2User, testCustomer);

        // Setup redirect strategy
        successHandler.setRedirectStrategy(redirectStrategy);
    }

    @Test
    void shouldGenerateJwtTokenAndRedirect() throws Exception {
        // Given
        var expectedToken = "generated-jwt-token";
        when(authentication.getPrincipal()).thenReturn(customOAuth2User);
        when(jwtUtil.generateToken("test@example.com")).thenReturn(expectedToken);
        when(response.isCommitted()).thenReturn(false);

        // When
        successHandler.onAuthenticationSuccess(request, response, authentication);

        // Then
        verify(jwtUtil).generateToken("test@example.com");
        verify(redirectStrategy).sendRedirect(eq(request), eq(response), eq("/oauth2/redirect?token=" + expectedToken));
    }

    @Test
    void shouldNotRedirectWhenResponseAlreadyCommitted() throws Exception {
        // Given
        when(response.isCommitted()).thenReturn(true);

        // When
        successHandler.onAuthenticationSuccess(request, response, authentication);

        // Then
        verify(jwtUtil, never()).generateToken(any());
        verify(redirectStrategy, never()).sendRedirect(any(), any(), any());
        // Authentication.getPrincipal() should not be called when response is committed
        verify(authentication, never()).getPrincipal();
    }

    @Test
    void shouldUseCustomerEmailForTokenGeneration() throws Exception {
        // Given
        var customerEmail = "custom@example.com";
        testCustomer.setEmail(customerEmail);

        when(authentication.getPrincipal()).thenReturn(customOAuth2User);
        when(jwtUtil.generateToken(customerEmail)).thenReturn("token");
        when(response.isCommitted()).thenReturn(false);

        // When
        successHandler.onAuthenticationSuccess(request, response, authentication);

        // Then
        verify(jwtUtil).generateToken(customerEmail);
    }

    @Test
    void shouldRedirectToCorrectEndpointWithToken() throws Exception {
        // Given
        var token = "test-jwt-token-12345";
        when(authentication.getPrincipal()).thenReturn(customOAuth2User);
        when(jwtUtil.generateToken(any())).thenReturn(token);
        when(response.isCommitted()).thenReturn(false);

        // When
        successHandler.onAuthenticationSuccess(request, response, authentication);

        // Then
        verify(redirectStrategy).sendRedirect(any(HttpServletRequest.class), any(HttpServletResponse.class), eq("/oauth2/redirect?token=" + token));
    }
}
