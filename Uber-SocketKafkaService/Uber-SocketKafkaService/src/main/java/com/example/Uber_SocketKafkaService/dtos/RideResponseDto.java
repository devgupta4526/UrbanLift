package com.example.Uber_SocketKafkaService.dtos;

import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RideResponseDto {

    private Boolean response;
    private Long bookingId;
    private Long driverId;

}

