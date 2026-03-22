package com.example.Uber_PaymentService.dtos;

import lombok.Data;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FareEstimateRequestDto {
    private double startLat;
    private double startLng;
    private double endLat;
    private double endLng;
    private String carType;
}