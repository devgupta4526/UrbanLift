package com.example.Uber_LocationService.controllers;


import com.example.Uber_LocationService.dtos.DriverLocationDto;
import com.example.Uber_LocationService.dtos.NearbyDriversRequestDto;
import com.example.Uber_LocationService.dtos.SaveDriverLocationRequestDto;
import com.example.Uber_LocationService.services.LocationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REMOVED: broad {@code try/catch (Exception)} on each endpoint that returned HTTP 500 with {@code false}
 * or an empty driver list. That hid validation and infrastructure errors and did not match a production
 * API error contract. Replaced with: Bean Validation on DTOs, checks in {@link com.example.Uber_LocationService.services.RedisLocationServiceImpl},
 * and {@link com.example.Uber_LocationService.api.GlobalExceptionHandler}.
 */
@RestController
@RequestMapping("/api/location")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @PostMapping("/drivers")
    public ResponseEntity<Boolean> saveDriverLocation(@Valid @RequestBody SaveDriverLocationRequestDto saveDriverLocationRequestDto) {
        Boolean response = locationService.saveDriverLocation(
                saveDriverLocationRequestDto.getDriverId(),
                saveDriverLocationRequestDto.getLatitude(),
                saveDriverLocationRequestDto.getLongitude());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/nearby/drivers")
    public ResponseEntity<List<DriverLocationDto>> getNearbyDrivers(@Valid @RequestBody NearbyDriversRequestDto nearbyDriversRequestDto) {
        List<DriverLocationDto> drivers = locationService.getNearbyDrivers(
                nearbyDriversRequestDto.getLatitude(),
                nearbyDriversRequestDto.getLongitude());
        return new ResponseEntity<>(drivers, HttpStatus.OK);
    }
}
