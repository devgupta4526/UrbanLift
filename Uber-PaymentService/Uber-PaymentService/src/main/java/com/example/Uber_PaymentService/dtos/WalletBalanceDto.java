package com.example.Uber_PaymentService.dtos;

import lombok.Data;

@Data
public class WalletBalanceDto {
    private double balance;
    private String currency;
}