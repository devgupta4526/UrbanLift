package com.example.Uber_SocketKafkaService.dtos;

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

