package com.example.Uber_PaymentService.dtos;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AddMoneyToWalletDto {
    private double amount;
}