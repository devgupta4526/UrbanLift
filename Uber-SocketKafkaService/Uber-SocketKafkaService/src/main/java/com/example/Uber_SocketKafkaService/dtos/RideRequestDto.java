package com.example.Uber_SocketKafkaService.dtos;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RideRequestDto {

    @NotNull(message = "bookingId is required")
    private Long bookingId;

    @NotNull(message = "passengerId is required")
    private Long passengerId;

    @DecimalMin(value = "-90.0", message = "pickupLat must be between -90 and 90")
    @DecimalMax(value = "90.0")
    private double pickupLat;
    @DecimalMin(value = "-180.0", message = "pickupLng must be between -180 and 180")
    @DecimalMax(value = "180.0")
    private double pickupLng;

    @DecimalMin(value = "-90.0", message = "dropLat must be between -90 and 90")
    @DecimalMax(value = "90.0")
    private double dropLat;
    @DecimalMin(value = "-180.0", message = "dropLng must be between -180 and 180")
    @DecimalMax(value = "180.0")
    private double dropLng;

    /** Optional: targeted drivers (fallback / testing) */
    private List<Long> driverIds;
}

