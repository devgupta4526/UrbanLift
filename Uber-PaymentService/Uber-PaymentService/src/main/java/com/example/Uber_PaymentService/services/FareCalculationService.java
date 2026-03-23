package com.example.Uber_PaymentService.services;

import com.example.Uber_EntityService.Models.CarType;
import com.example.Uber_EntityService.Models.FareConfig;
import com.example.Uber_PaymentService.dtos.FareEstimateDto;
import com.example.Uber_PaymentService.dtos.FareEstimateRequestDto;
import com.example.Uber_PaymentService.repositories.FareConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class FareCalculationService {

    @Autowired
    private FareConfigRepository fareConfigRepository;

    public FareEstimateDto estimateFare(FareEstimateRequestDto request) {
        double distance = calculateDistance(
                request.getStartLat(), request.getStartLng(),
                request.getEndLat(), request.getEndLng()
        );

        final CarType carType;
        try {
            carType = CarType.valueOf(request.getCarType());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid car type: " + request.getCarType(), ex);
        }
        FareConfig config = fareConfigRepository.findByCarType(carType)
                .orElseThrow(() -> new IllegalArgumentException("Fare config not found for car type: " + request.getCarType()));

        BigDecimal baseFare        = config.getBaseFare();
        BigDecimal perKmRate       = config.getPerKmRate();
        BigDecimal surgeMultiplier = config.getSurgeMultiplier();

        BigDecimal distanceFare = perKmRate.multiply(BigDecimal.valueOf(distance))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal timeFare     = BigDecimal.ZERO;
        BigDecimal totalFare    = baseFare.add(distanceFare).add(timeFare)
                .multiply(surgeMultiplier)
                .setScale(2, RoundingMode.HALF_UP);

        FareEstimateDto dto = new FareEstimateDto();
        dto.setEstimatedFare(totalFare);
        dto.setBaseFare(baseFare);
        dto.setDistanceFare(distanceFare);
        dto.setTimeFare(timeFare);
        dto.setSurgeMultiplier(surgeMultiplier);
        dto.setTotalFare(totalFare);

        return dto;
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; // result directly in km
    }
}