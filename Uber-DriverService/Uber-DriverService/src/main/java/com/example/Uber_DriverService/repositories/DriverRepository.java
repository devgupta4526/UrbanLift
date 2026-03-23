package com.example.Uber_DriverService.repositories;

import com.example.Uber_EntityService.Models.Driver;
import com.example.Uber_EntityService.Models.DriverApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DriverRepository extends JpaRepository<Driver, Long> {

    @Query("SELECT d FROM Driver d WHERE LOWER(TRIM(d.email)) = LOWER(TRIM(:email))")
    Optional<Driver> findByEmail(@Param("email") String email);

    List<Driver> findByDriverApprovalStatus(DriverApprovalStatus status);

    List<Driver> findByIsAvailableTrue();
}
