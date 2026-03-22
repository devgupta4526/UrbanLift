package com.example.Uber_PaymentService.dtos;

import lombok.Data;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WalletBalanceDto {
    private BigDecimal balance;
    private String currency;
}