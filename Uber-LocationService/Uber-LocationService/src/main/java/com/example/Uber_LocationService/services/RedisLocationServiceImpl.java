package com.example.Uber_LocationService.services;

import com.example.Uber_LocationService.dtos.DriverLocationDto;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;

public class RedisLocationServiceImpl implements LocationService{

    private static final String DRIVER_GEO_OPS_KEY = "drivers";
    private static final Double SEARCH_RADIUS = 5.0;

    private final StringRedisTemplate stringRedisTemplate;

    public RedisLocationServiceImpl(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }


    @Override
    public Boolean saveDriverLocation(String driverId, Double latitude, Double longitude) {
        GeoOperations<String,String> geoOperations =stringRedisTemplate.opsForGeo();
        geoOperations.add(
                DRIVER_GEO_OPS_KEY,
                new RedisGeoCommands.GeoLocation<>(
                        driverId,
                        new Point(
                                latitude,
                                longitude
                        )
                )
        );
        return true;
    }

    @Override
    public List<DriverLocationDto> getNearbyDrivers(Double latitude, Double longitude) {
       GeoOperations<String,String> geoOperations =stringRedisTemplate.opsForGeo();
        Distance radius = new Distance(SEARCH_RADIUS, Metrics.KILOMETERS);
        Circle within = new Circle(new Point(latitude, longitude), radius);

        GeoResults<RedisGeoCommands.GeoLocation<String>> results = geoOperations.radius(DRIVER_GEO_OPS_KEY, within);
        List<DriverLocationDto> drivers = new ArrayList<>();

        for(GeoResult<RedisGeoCommands.GeoLocation<String>> result : results){
            Point point = geoOperations.position(DRIVER_GEO_OPS_KEY,result.getContent().getName()).get(0);
            DriverLocationDto driverLocationDto = DriverLocationDto.builder()
                    .driverId(result.getContent().getName())
                    .latitude(point.getX())
                    .longitude(point.getY())
                    .build();
            drivers.add(driverLocationDto);
        }
        return drivers;
    }
}
