package com.example.Uber_BookingService.dtos;

import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RideResponseDto {

    public Boolean response;
    public Long bookingId;

}

