package com.example.Uber_PaymentService.dtos;

import lombok.Data;

import java.math.BigDecimal;

/** Kafka payload from Booking service — keep fields aligned with booking-completed-topic JSON. */
@Data
public class BookingCompletedEventDto {
    private Long bookingId;
    private Long passengerId;
    private Long driverId;
    private BigDecimal totalDistance;
    private BigDecimal fare;
}
