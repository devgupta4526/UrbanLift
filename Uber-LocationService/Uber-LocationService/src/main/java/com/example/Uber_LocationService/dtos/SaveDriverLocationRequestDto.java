package com.example.Uber_LocationService.dtos;


import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveDriverLocationRequestDto {
    @NotBlank(message = "driverId is required")
    String driverId;
    @NotNull @DecimalMin("-90.0") @DecimalMax("90.0")
    Double latitude;
    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0")
    Double longitude;
}