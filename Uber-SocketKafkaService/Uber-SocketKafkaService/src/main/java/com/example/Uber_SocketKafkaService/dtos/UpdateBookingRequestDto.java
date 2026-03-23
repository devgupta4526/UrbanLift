package com.example.Uber_SocketKafkaService.dtos;

import lombok.*;

/**
 * Plain {@code Long} so RestTemplate/Jackson JSON matches Booking API ({@code "driverId": 1}).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateBookingRequestDto {

    private String status;
    private Long driverId;

}

