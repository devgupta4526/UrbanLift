package com.example.Uber_PaymentService.dtos;

import lombok.Data;

@Data
public class PaymentCompletedEventDto {
    private Long bookingId;
    private Long paymentId;
    private String status;
}