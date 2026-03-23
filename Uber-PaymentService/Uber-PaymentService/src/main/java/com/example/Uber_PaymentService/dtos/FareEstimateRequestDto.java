package com.example.Uber_PaymentService.dtos;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FareEstimateRequestDto {
    @DecimalMin("-90.0") @DecimalMax("90.0")
    private double startLat;
    @DecimalMin("-180.0") @DecimalMax("180.0")
    private double startLng;
    @DecimalMin("-90.0") @DecimalMax("90.0")
    private double endLat;
    @DecimalMin("-180.0") @DecimalMax("180.0")
    private double endLng;
    @NotBlank(message = "carType is required (e.g. SEDAN)")
    private String carType;
}