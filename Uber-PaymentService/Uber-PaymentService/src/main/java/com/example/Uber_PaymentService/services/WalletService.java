package com.example.Uber_PaymentService.services;

import com.example.Uber_EntityService.Models.Wallet;
import com.example.Uber_EntityService.Models.Wallet.UserType;
import com.example.Uber_PaymentService.dtos.WalletBalanceDto;
import com.example.Uber_PaymentService.repositories.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class WalletService {

    @Autowired
    private WalletRepository walletRepository;

    public WalletBalanceDto getBalance(Long userId, String userType) {
        UserType type = UserType.valueOf(userType.toUpperCase());
        Wallet wallet = walletRepository.findByUserIdAndUserType(userId, type)
                .orElseGet(() -> createWallet(userId, type));

        WalletBalanceDto dto = new WalletBalanceDto();
        dto.setBalance(wallet.getBalance());
        dto.setCurrency(wallet.getCurrency());
        return dto;
    }

    public void addMoney(Long userId, String userType, double amount) {
        UserType type = UserType.valueOf(userType.toUpperCase());
        Wallet wallet = walletRepository.findByUserIdAndUserType(userId, type)
                .orElseGet(() -> createWallet(userId, type));

        wallet.setBalance(wallet.getBalance().add(BigDecimal.valueOf(amount)));
        walletRepository.save(wallet);
    }

    public void deductMoney(Long userId, String userType, double amount) {
        UserType type = UserType.valueOf(userType.toUpperCase());
        Wallet wallet = walletRepository.findByUserIdAndUserType(userId, type)
                .orElseThrow(() -> new RuntimeException("Wallet not found for userId: " + userId));

        if (wallet.getBalance().compareTo(BigDecimal.valueOf(amount)) < 0) {
            throw new RuntimeException("Insufficient balance in wallet for userId: " + userId);
        }


        wallet.setBalance(wallet.getBalance().subtract(BigDecimal.valueOf(amount)));
        walletRepository.save(wallet);
    }

    public void creditMoney(Long userId, String userType, double amount) {
        UserType type = UserType.valueOf(userType.toUpperCase());
        Wallet wallet = walletRepository.findByUserIdAndUserType(userId, type)
                .orElseGet(() -> createWallet(userId, type));


        wallet.setBalance(wallet.getBalance().add(BigDecimal.valueOf(amount)));
        walletRepository.save(wallet);
    }

    private Wallet createWallet(Long userId, UserType userType) {
        Wallet wallet = new Wallet();
        wallet.setUserId(userId);
        wallet.setUserType(userType);
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setCurrency("INR");
        return walletRepository.save(wallet);
    }
}