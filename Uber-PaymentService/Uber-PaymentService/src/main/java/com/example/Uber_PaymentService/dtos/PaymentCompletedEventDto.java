package com.example.Uber_PaymentService.dtos;

import lombok.Data;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentCompletedEventDto {
    private Long bookingId;
    private Long paymentId;
    private String status;
}