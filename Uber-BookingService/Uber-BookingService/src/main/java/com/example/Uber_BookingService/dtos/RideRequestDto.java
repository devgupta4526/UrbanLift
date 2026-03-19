package com.example.Uber_BookingService.dtos;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RideRequestDto {

    private Long bookingId;

    private Long passengerId;

    // 📍 Location-based matching (PRIMARY)
    private double pickupLat;
    private double pickupLng;

    private double dropLat;
    private double dropLng;

    // 🚗 Optional: targeted drivers (fallback / testing)
    private List<Long> driverIds;
}

