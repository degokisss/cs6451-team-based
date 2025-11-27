package com.example.hotelreservationsystem.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for booking cancellation
 * Contains optional cancellation reason
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancellationRequest {

    @Size(max = 500, message = "Cancellation reason must not exceed 500 characters")
    private String reason;
}
