package com.example.hotelreservationsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RoomTypeCreateRequest {
    @NotBlank(message = "Name cannot be blank")
    private String name;

    private String description;

    @NotNull(message = "Price cannot be null")
    private Float price;

    @NotNull(message = "Capacity cannot be null")
    private Integer capacity;
}
