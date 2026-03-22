package com.example.Uber_PaymentService.dtos;

import lombok.Data;

import java.time.LocalDateTime;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookingSummaryDto {
    private Long bookingId;
    private LocalDateTime bookingDate;
    private double amount;
    private String status;
}