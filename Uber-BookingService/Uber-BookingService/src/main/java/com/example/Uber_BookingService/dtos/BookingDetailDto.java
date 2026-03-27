package com.example.Uber_BookingService.dtos;

import com.example.Uber_EntityService.Models.ExactLocation;
import lombok.*;

import java.util.Date;

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
    private BookingParticipantDto passenger;
    private BookingParticipantDto driver;
    private ExactLocation startLocation;
    private ExactLocation endLocation;
}
