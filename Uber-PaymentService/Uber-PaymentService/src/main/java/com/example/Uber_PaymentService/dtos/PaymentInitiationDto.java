package com.example.Uber_PaymentService.dtos;

import lombok.Data;

@Data
public class PaymentInitiationDto {
    private Long bookingId;
    private double amount;
}