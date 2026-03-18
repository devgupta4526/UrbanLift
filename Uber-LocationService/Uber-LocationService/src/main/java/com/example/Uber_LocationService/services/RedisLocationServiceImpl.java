package com.example.Uber_LocationService.services;

import com.example.Uber_LocationService.dtos.DriverLocationDto;

import java.util.List;

public class RedisLocationServiceImpl implements LocationService{

    private static final String DRIVER_GEO_OPS_KEY = "drivers";
    private static final Double SEARCH_RADIUS = 5.0;
    
    @Override
    public Boolean saveDriverLocation(String driverId, Double latitude, Double longitude) {
        return null;
    }

    @Override
    public List<DriverLocationDto> getNearbyDrivers(Double latitude, Double longitude) {
        return List.of();
    }
}
