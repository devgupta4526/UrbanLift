package com.example.Uber_BookingService.services;

import com.example.Uber_BookingService.dtos.*;
import org.springframework.stereotype.Service;



public interface BookingService {

    CreateBookingResponseDto createBooking(CreateBookingDto bookingDetails);

    UpdateBookingResponseDto updateBooking(UpdateBookingRequestDto bookingRequestDto, Long bookingId);

    BookingDetailDto getBookingById(Long bookingId);
}

