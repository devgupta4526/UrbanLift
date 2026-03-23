package com.example.Uber_PaymentService.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentInitiationDto {
    @NotNull
    private Long bookingId;
    @Positive(message = "amount must be positive")
    private double amount;
}