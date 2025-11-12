package com.example.hotelreservationsystem.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        if (response.isCommitted()) {
            logger.debug("Response has already been committed. Unable to redirect.");
            return;
        }

        // Get the authenticated user
        var oauth2User = (CustomOAuth2User) authentication.getPrincipal();
        var email = oauth2User.customer().getEmail();

        // Generate JWT token
        var token = jwtUtil.generateToken(email);

        // Redirect to frontend with token
        // For development, you can redirect to a simple page that displays the token
        // In production, redirect to your frontend application with the token as a query parameter
        var targetUrl = UriComponentsBuilder.fromUriString("/oauth2/redirect")
                                            .queryParam("token", token)
                                            .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
