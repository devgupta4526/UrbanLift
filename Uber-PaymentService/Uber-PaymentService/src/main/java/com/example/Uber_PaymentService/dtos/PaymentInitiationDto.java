package com.example.Uber_PaymentService.dtos;

import lombok.Data;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentInitiationDto {
    private Long bookingId;
    private double amount;
}