package com.example.Uber_BookingService.services;

import com.example.Uber_BookingService.dtos.CreateBookingDto;
import com.example.Uber_BookingService.dtos.CreateBookingResponseDto;
import com.example.Uber_BookingService.dtos.UpdateBookingRequestDto;
import com.example.Uber_BookingService.dtos.UpdateBookingResponseDto;

public interface BookingService {

    CreateBookingResponseDto createBooking(CreateBookingDto bookingDetails);

    UpdateBookingResponseDto updateBooking(UpdateBookingRequestDto bookingRequestDto, Long bookingId);
}

