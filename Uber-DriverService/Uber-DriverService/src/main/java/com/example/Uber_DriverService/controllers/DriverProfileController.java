package com.example.Uber_DriverService.controllers;

import com.example.Uber_DriverService.dtos.DriverDto;
import com.example.Uber_DriverService.services.DriverAuthService;
import com.example.Uber_EntityService.Models.Driver;
import com.example.Uber_DriverService.repositories.DriverRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/driver")
public class DriverProfileController {

    private final DriverRepository driverRepository;
    private final DriverAuthService driverAuthService;

    public DriverProfileController(DriverRepository driverRepository, DriverAuthService driverAuthService) {
        this.driverRepository = driverRepository;
        this.driverAuthService = driverAuthService;
    }

    @GetMapping("/profile")
    public ResponseEntity<DriverDto> getProfile(Authentication auth) {
        Driver driver = getDriverFromAuth(auth);
        return ResponseEntity.ok(driverAuthService.toDto(driver));
    }

    private Driver getDriverFromAuth(Authentication auth) {
        String email = (String) auth.getPrincipal();
        return driverRepository.findByEmailWithCar(email)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found: " + email));
    }
}
