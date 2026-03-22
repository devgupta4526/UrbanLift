package com.example.Uber_PaymentService.repositories;

import com.example.Uber_EntityService.Models.FareConfig;
import com.example.Uber_EntityService.Models.CarType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FareConfigRepository extends JpaRepository<FareConfig, Long> {
    Optional<FareConfig> findByCarType(CarType carType);
}