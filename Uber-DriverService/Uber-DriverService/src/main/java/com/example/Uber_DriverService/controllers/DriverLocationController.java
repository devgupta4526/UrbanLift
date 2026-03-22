package com.example.Uber_DriverService.controllers;

import com.example.Uber_DriverService.dtos.UpdateLocationRequestDto;
import com.example.Uber_DriverService.services.DriverLocationService;
import com.example.Uber_EntityService.Models.Driver;
import com.example.Uber_DriverService.repositories.DriverRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/driver")
public class DriverLocationController {

    private final DriverLocationService driverLocationService;
    private final DriverRepository driverRepository;

    public DriverLocationController(DriverLocationService driverLocationService, DriverRepository driverRepository) {
        this.driverLocationService = driverLocationService;
        this.driverRepository = driverRepository;
    }

    @PostMapping("/location")
    public ResponseEntity<Void> updateLocation(@RequestBody UpdateLocationRequestDto dto, Authentication auth) {
        Driver driver = getDriverFromAuth(auth);
        driverLocationService.updateLocation(driver.getId(), dto.getLatitude(), dto.getLongitude());
        return ResponseEntity.ok().build();
    }

    private Driver getDriverFromAuth(Authentication auth) {
        String email = (String) auth.getPrincipal();
        return driverRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found: " + email));
    }
}
