package com.example.Uber_PaymentService.dtos;

import lombok.Data;

import java.time.LocalDateTime;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
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