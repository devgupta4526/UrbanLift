package com.example.Uber_PaymentService.dtos;

import lombok.Data;

import java.math.BigDecimal;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookingCompletedEventDto {
    private Long bookingId;
    private Long passengerId;
    private Long driverId;
    private BigDecimal totalDistance;
    private BigDecimal fare;
}