package com.example.Uber_PaymentService.controllers;

import com.example.Uber_PaymentService.dtos.AddMoneyToWalletDto;
import com.example.Uber_PaymentService.dtos.WalletBalanceDto;
import com.example.Uber_PaymentService.services.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
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
    public ResponseEntity<Void> addMoney(@RequestParam Long userId, @RequestParam String userType, @RequestBody AddMoneyToWalletDto request) {
        walletService.addMoney(userId, userType, request.getAmount());
        return ResponseEntity.ok().build();
    }
}