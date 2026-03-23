package com.example.Uber_BookingService.dtos;

import lombok.*;

import java.util.Optional;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateBookingRequestDto {

    private String status;
    private Long driverId;

}