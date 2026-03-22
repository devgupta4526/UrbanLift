package com.example.Uber_DriverService.controllers;

import com.example.Uber_DriverService.dtos.AvailabilityRequestDto;
import com.example.Uber_DriverService.services.DriverAvailabilityService;
import com.example.Uber_EntityService.Models.Driver;
import com.example.Uber_DriverService.repositories.DriverRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/driver")
public class DriverAvailabilityController {

    private final DriverAvailabilityService driverAvailabilityService;
    private final DriverRepository driverRepository;

    public DriverAvailabilityController(DriverAvailabilityService driverAvailabilityService, DriverRepository driverRepository) {
        this.driverAvailabilityService = driverAvailabilityService;
        this.driverRepository = driverRepository;
    }

    @PutMapping("/availability")
    public ResponseEntity<Void> setAvailability(@RequestBody AvailabilityRequestDto dto,
                                                @RequestParam(required = false) Double lat,
                                                @RequestParam(required = false) Double lng,
                                                Authentication auth) {
        Driver driver = getDriverFromAuth(auth);
        driverAvailabilityService.setAvailability(driver.getId(), dto.getAvailable(), lat, lng);
        return ResponseEntity.ok().build();
    }

    private Driver getDriverFromAuth(Authentication auth) {
        String email = (String) auth.getPrincipal();
        return driverRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found: " + email));
    }
}
