package com.example.Uber_SocketKafkaService.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RideLocationUpdateDto {
    @NotNull
    private Long bookingId;

    @NotNull
    private Long driverId;

    @NotNull
    @Min(-90)
    @Max(90)
    private Double latitude;

    @NotNull
    @Min(-180)
    @Max(180)
    private Double longitude;

    /** epoch millis; optional from client */
    private Long timestamp;
}
