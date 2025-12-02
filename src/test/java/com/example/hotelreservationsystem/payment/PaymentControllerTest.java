package com.example.hotelreservationsystem.payment;

import com.example.hotelreservationsystem.dto.PaymentRequest;
import com.example.hotelreservationsystem.dto.PaymentResponse;
import com.example.hotelreservationsystem.enums.PaymentType;
import com.example.hotelreservationsystem.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest

@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentService paymentServiceMock;

    @Test
    void shouldReturn200AndSuccessResponse_WhenPaymentSuccessful() throws Exception {
        // Given
        Long orderId = 123L;
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(orderId);
        request.setPaymentType(PaymentType.CREDIT_CARD);

        PaymentResponse mockResponse = PaymentResponse.builder()
                .orderId(orderId)
                .status("SUCCESS")
                .transactionId("mock_tx_id")
                .message("Payment Successful")
                .build();

        // Mock service behavior
        when(paymentServiceMock.executePayment(eq(orderId), eq(PaymentType.CREDIT_CARD)))
                .thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(post("/api/payment/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()) // Expect 200 OK
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.transactionId").value("mock_tx_id"));
    }

    @Test
    void shouldReturnBadRequest_WhenPaymentFails() throws Exception {
        // Given
        Long orderId = 456L;
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(orderId);
        request.setPaymentType(PaymentType.PAYPAL);

        PaymentResponse failureResponse = PaymentResponse.builder()
                .status("FAILED")
                .message("Validation Failed")
                .build();

        // Mock service behavior
        when(paymentServiceMock.executePayment(eq(orderId), any(PaymentType.class)))
                .thenReturn(failureResponse);

        // When & Then
        mockMvc.perform(post("/api/payment/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()) // Expect 400 Bad Request
                .andExpect(jsonPath("$.status").value("FAILED"));
    }
}