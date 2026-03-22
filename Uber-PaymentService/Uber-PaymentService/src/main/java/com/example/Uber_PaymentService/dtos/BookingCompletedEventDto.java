package com.example.Uber_PaymentService.dtos;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BookingCompletedEventDto {
    private Long bookingId;
    private Long passengerId;
    private Long driverId;
    private BigDecimal totalDistance;
    private BigDecimal fare;
}