package com.example.Uber_DriverService.dtos;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarDto {
    private String plateNumber;
    private String colorName;
    private String brand;
    private String model;
    private String carType;
}
