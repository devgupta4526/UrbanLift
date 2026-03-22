package com.example.Uber_PaymentService.dtos;

import lombok.Data;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentConfirmDto {
    private Long paymentId;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;
}