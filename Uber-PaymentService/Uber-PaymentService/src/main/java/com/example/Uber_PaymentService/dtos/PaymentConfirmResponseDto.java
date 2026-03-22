package com.example.Uber_PaymentService.dtos;

import lombok.Data;

@Data
public class PaymentConfirmResponseDto {
    private boolean success;        // ✅ missing
    private String paymentId;
    private String status;
    private double amount;


}