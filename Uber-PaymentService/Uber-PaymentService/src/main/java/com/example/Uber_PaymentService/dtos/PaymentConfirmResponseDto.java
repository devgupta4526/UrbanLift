package com.example.Uber_PaymentService.dtos;

import lombok.Data;

@Data
public class PaymentConfirmResponseDto {
    private Long paymentId;
    private String status;
    private double amount;
}