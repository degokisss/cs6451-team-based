package com.example.hotelreservationsystem.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-client-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-client-secret"
})
class OAuth2ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnTokenInformationWhenTokenProvided() throws Exception {
        // Given
        var testToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test";

        // When & Then
        mockMvc.perform(get("/oauth2/redirect").param("token", testToken))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.token", is(testToken)))
               .andExpect(jsonPath("$.type", is("Bearer")))
               .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldReturnBearerTypeInResponse() throws Exception {
        // Given
        var testToken = "test-token-123";

        // When & Then
        mockMvc.perform(get("/oauth2/redirect").param("token", testToken))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.type", is("Bearer")));
    }

    @Test
    void shouldReturnHelpfulMessage() throws Exception {
        // Given
        var testToken = "test-token-456";

        // When & Then
        mockMvc.perform(get("/oauth2/redirect").param("token", testToken))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.message").value("OAuth2 authentication successful. Use this token in the Authorization header for API requests."));
    }

    @Test
    void shouldHandleEmptyToken() throws Exception {
        // When & Then
        mockMvc.perform(get("/oauth2/redirect").param("token", ""))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.token", is("")));
    }

    @Test
    void shouldReturnJsonContentType() throws Exception {
        // Given
        var testToken = "test-token-789";

        // When & Then
        mockMvc.perform(get("/oauth2/redirect").param("token", testToken))
               .andExpect(status().isOk())
               .andExpect(content().contentType("application/json"));
    }
}
