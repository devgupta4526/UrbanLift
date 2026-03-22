package com.example.Uber_DriverService.services;

import com.example.Uber_EntityService.Models.Driver;
import com.example.Uber_DriverService.repositories.DriverRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class DriverAvailabilityService {

    private final DriverRepository driverRepository;
    private final DriverLocationService driverLocationService;

    public DriverAvailabilityService(DriverRepository driverRepository, DriverLocationService driverLocationService) {
        this.driverRepository = driverRepository;
        this.driverLocationService = driverLocationService;
    }

    public void setAvailability(Long driverId, boolean available, Double latitude, Double longitude) {
        Optional<Driver> driverOpt = driverRepository.findById(driverId);
        if (driverOpt.isEmpty()) {
            throw new IllegalArgumentException("Driver not found: " + driverId);
        }

        Driver driver = driverOpt.get();
        driver.setAvailable(available);
        driverRepository.save(driver);

        if (available && latitude != null && longitude != null) {
            driverLocationService.updateLocation(driverId, latitude, longitude);
        }
    }
}
