package com.example.Uber_BookingService.dtos;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateBookingResponseDto {

    private Long bookingId;
    private String status;
    private BookingParticipantDto driver;

}
