package com.example.Uber_DriverService.services;

import com.example.Uber_DriverService.apis.LocationServiceApi;
import com.example.Uber_DriverService.dtos.SaveDriverLocationRequestDto;
import com.example.Uber_EntityService.Models.Driver;
import com.example.Uber_DriverService.repositories.DriverRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class DriverLocationService {

    private final DriverRepository driverRepository;
    private final LocationServiceApi locationServiceApi;

    public DriverLocationService(DriverRepository driverRepository, LocationServiceApi locationServiceApi) {
        this.driverRepository = driverRepository;
        this.locationServiceApi = locationServiceApi;
    }

    public void updateLocation(Long driverId, Double latitude, Double longitude) {
        Optional<Driver> driverOpt = driverRepository.findById(driverId);
        if (driverOpt.isEmpty()) {
            throw new IllegalArgumentException("Driver not found: " + driverId);
        }

        SaveDriverLocationRequestDto request = SaveDriverLocationRequestDto.builder()
                .driverId(String.valueOf(driverId))
                .latitude(latitude)
                .longitude(longitude)
                .build();

        try {
            locationServiceApi.saveDriverLocation(request).execute();
        } catch (Exception e) {
            throw new RuntimeException("Failed to sync location to LocationService", e);
        }
    }
}
