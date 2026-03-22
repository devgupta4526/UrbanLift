package com.example.Uber_PaymentService.repositories;


import com.example.Uber_EntityService.Models.Wallet;
import com.example.Uber_EntityService.Models.Wallet.UserType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByUserIdAndUserType(Long userId, UserType userType);
}