package com.example.Uber_BookingService.dtos;

import com.example.Uber_EntityService.Models.Driver;
import com.example.Uber_EntityService.Models.ExactLocation;
import com.example.Uber_EntityService.Models.Passenger;
import lombok.*;

import java.util.Date;
import java.util.Optional;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookingDetailDto {
    private Long id;
    private String bookingStatus;
    private Date bookingDate;
    private Date startTime;
    private Date endTime;
    private Long totalDistance;
    private Optional<Passenger> passenger;
    private Optional<Driver> driver;
    private ExactLocation startLocation;
    private ExactLocation endLocation;
}
