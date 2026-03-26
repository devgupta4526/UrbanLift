package com.example.Uber_BookingService.dtos;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TripRatingSummaryDto {
    private Integer passengerToDriverScore;
    private String passengerToDriverComment;
    private Integer driverToPassengerScore;
    private String driverToPassengerComment;
}
