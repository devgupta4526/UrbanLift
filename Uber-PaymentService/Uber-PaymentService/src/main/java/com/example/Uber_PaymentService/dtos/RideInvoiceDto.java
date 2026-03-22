package com.example.Uber_PaymentService.dtos;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RideInvoiceDto {
    private Long bookingId;
    private double baseFare;
    private double distanceFare;
    private double timeFare;
    private double surgeMultiplier;
    private double totalFare;
    private double commission;
    private double driverEarnings;
    private LocalDateTime paymentTime;
}