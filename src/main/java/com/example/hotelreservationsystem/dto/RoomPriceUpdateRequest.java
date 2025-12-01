package com.example.hotelreservationsystem.dto;

import com.example.hotelreservationsystem.enums.RoomPriceTier;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for updating room type price
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomPriceUpdateRequest {

    @NotNull(message = "Room type ID is required")
    private Long roomTypeId;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    private Float price;

    private List<RoomPriceTier> priceTiers;
}
