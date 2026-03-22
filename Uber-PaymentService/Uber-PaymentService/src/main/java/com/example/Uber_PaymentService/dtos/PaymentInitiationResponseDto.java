package com.example.Uber_PaymentService.dtos;

import lombok.Data;

@Data
public class PaymentInitiationResponseDto {
    private String orderId;
    private double amount;
    private String currency;
    private String status;
}