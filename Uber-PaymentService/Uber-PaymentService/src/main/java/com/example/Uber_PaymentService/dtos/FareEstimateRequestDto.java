package com.example.Uber_PaymentService.dtos;

import lombok.Data;

@Data
public class FareEstimateRequestDto {
    private double startLat;
    private double startLng;
    private double endLat;
    private double endLng;
    private String carType;
}