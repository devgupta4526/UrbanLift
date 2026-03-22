package com.example.Uber_DriverService.repositories;

import com.example.Uber_EntityService.Models.Driver;
import com.example.Uber_EntityService.Models.DriverApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DriverRepository extends JpaRepository<Driver, Long> {

    Optional<Driver> findByEmail(String email);

    List<Driver> findByDriverApprovalStatus(DriverApprovalStatus status);

    List<Driver> findByIsAvailableTrue();
}
