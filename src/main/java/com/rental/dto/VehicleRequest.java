package com.rental.dto;

import com.rental.entity.enums.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class VehicleRequest {
    @NotBlank
    private String name;

    @NotNull
    private VehicleType type;

    private String model;

    @NotBlank
    private String registrationNumber;

    @NotNull
    private BigDecimal pricePerDay;

    private String imageUrl;
}
