package com.example.Uber_PaymentService.dtos;

import lombok.Data;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentInitiationResponseDto {
    /** Internal id — use with POST /payment/confirm */
    private Long paymentId;
    private String orderId;
    private double amount;
    private String currency;
    private String status;
}