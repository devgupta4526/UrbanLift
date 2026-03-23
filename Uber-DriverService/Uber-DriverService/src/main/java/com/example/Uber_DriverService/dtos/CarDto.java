package com.example.Uber_DriverService.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarDto {
    @NotBlank @Size(max = 32)
    private String plateNumber;
    @NotBlank @Size(max = 64)
    private String colorName;
    @NotBlank @Size(max = 64)
    private String brand;
    @NotBlank @Size(max = 64)
    private String model;
    @Size(max = 32)
    private String carType;
}
