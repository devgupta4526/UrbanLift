package com.example.Uber_DriverService.services;

import com.example.Uber_DriverService.apis.LocationServiceApi;
import com.example.Uber_DriverService.dtos.SaveDriverLocationRequestDto;
import com.example.Uber_EntityService.Models.Driver;
import com.example.Uber_DriverService.repositories.DriverRepository;
import org.springframework.stereotype.Service;
import retrofit2.Response;

import java.io.IOException;
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

        // REMOVED: catch (Exception) + RuntimeException — use typed Retrofit/IO handling and IllegalStateException for API contract.
        try {
            Response<Boolean> response = locationServiceApi.saveDriverLocation(request).execute();
            if (!response.isSuccessful()) {
                throw new IllegalStateException("LocationService returned HTTP " + response.code());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to reach LocationService", e);
        }
    }
}
