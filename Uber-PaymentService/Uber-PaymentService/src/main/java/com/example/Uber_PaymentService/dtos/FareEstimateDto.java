package com.example.Uber_PaymentService.dtos;

import lombok.Data;

@Data
public class FareEstimateDto {
    private double estimatedFare;
    private double baseFare;
    private double distanceFare;
    private double timeFare;
    private double surgeMultiplier;
    private double totalFare;
}