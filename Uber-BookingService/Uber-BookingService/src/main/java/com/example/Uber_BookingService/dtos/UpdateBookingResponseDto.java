package com.example.Uber_BookingService.dtos;

import lombok.*;
import com.example.Uber_EntityService.Models.BookingStatus;
import com.example.Uber_EntityService.Models.Driver;
import java.util.Optional;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateBookingResponseDto {

    private Long bookingId;
    private BookingStatus status;
    private Optional<Driver> driver;

}
