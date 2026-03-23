package com.example.Uber_LocationService.services;

import com.example.Uber_LocationService.dtos.DriverLocationDto;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;


@Service
public class RedisLocationServiceImpl implements LocationService {

    private static final String DRIVER_GEO_OPS_KEY = "drivers";
    private static final Double SEARCH_RADIUS = 5.0;

    private final StringRedisTemplate stringRedisTemplate;

    public RedisLocationServiceImpl(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public Boolean saveDriverLocation(String driverId, Double latitude, Double longitude) {
        if (!StringUtils.hasText(driverId)) {
            throw new IllegalArgumentException("driverId is required");
        }
        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("latitude and longitude are required");
        }
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("coordinates out of valid range");
        }
        // Spring Data Redis Point is x = longitude, y = latitude (matches Redis GEO / WGS84).
        GeoOperations<String, String> geoOperations = stringRedisTemplate.opsForGeo();
        geoOperations.add(
                DRIVER_GEO_OPS_KEY,
                new RedisGeoCommands.GeoLocation<>(
                        driverId,
                        new Point(longitude, latitude)
                )
        );
        return true;
    }

    @Override
    public List<DriverLocationDto> getNearbyDrivers(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("latitude and longitude are required");
        }
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("coordinates out of valid range");
        }
        GeoOperations<String, String> geoOperations = stringRedisTemplate.opsForGeo();
        Distance radius = new Distance(SEARCH_RADIUS, Metrics.KILOMETERS);
        Circle within = new Circle(new Point(longitude, latitude), radius);

        GeoResults<RedisGeoCommands.GeoLocation<String>> results = geoOperations.radius(DRIVER_GEO_OPS_KEY, within);
        List<DriverLocationDto> drivers = new ArrayList<>();
        if (results == null) {
            return drivers;
        }

        for (GeoResult<RedisGeoCommands.GeoLocation<String>> result : results) {
            String name = result.getContent().getName();
            List<Point> positions = geoOperations.position(DRIVER_GEO_OPS_KEY, name);
            if (positions == null || positions.isEmpty()) {
                continue;
            }
            Point point = positions.get(0);
            DriverLocationDto driverLocationDto = DriverLocationDto.builder()
                    .driverId(name)
                    .latitude(point.getY())
                    .longitude(point.getX())
                    .build();
            drivers.add(driverLocationDto);
        }
        return drivers;
    }
}
