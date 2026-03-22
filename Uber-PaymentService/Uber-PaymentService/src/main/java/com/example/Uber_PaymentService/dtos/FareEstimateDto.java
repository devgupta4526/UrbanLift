package com.example.Uber_PaymentService.dtos;

import lombok.Data;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FareEstimateDto {
    private double estimatedFare;
    private double baseFare;
    private double distanceFare;
    private double timeFare;
    private double surgeMultiplier;
    private double totalFare;
}