package com.example.Uber_PaymentService.controllers;

import com.example.Uber_PaymentService.dtos.AddMoneyToWalletDto;
import com.example.Uber_PaymentService.dtos.WalletBalanceDto;
import com.example.Uber_PaymentService.services.WalletService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/v1/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/balance")
    public ResponseEntity<WalletBalanceDto> getBalance(@RequestParam Long userId, @RequestParam String userType) {
        WalletBalanceDto balance = walletService.getBalance(userId, userType);
        return ResponseEntity.ok(balance);
    }

    @PostMapping("/add")
    public ResponseEntity<Void> addMoney(
            @RequestParam @NotNull @Positive Long userId,
            @RequestParam @NotBlank String userType,
            @Valid @RequestBody AddMoneyToWalletDto request) {
        walletService.addMoney(userId, userType, request.getAmount());
        return ResponseEntity.ok().build();
    }
}