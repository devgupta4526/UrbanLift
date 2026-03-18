package com.example.Uber_LocationService.services;


import com.example.Uber_LocationService.dtos.DriverLocationDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface LocationService {

    Boolean saveDriverLocation(String driverId, Double latitude, Double longitude);

    List<DriverLocationDto> getNearbyDrivers(Double latitude, Double longitude);
}
