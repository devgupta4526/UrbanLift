package com.example.Uber_BookingService.services;

import com.example.Uber_BookingService.dtos.*;
import org.springframework.stereotype.Service;

import java.util.List;


public interface BookingService {

    CreateBookingResponseDto createBooking(CreateBookingDto bookingDetails);

    UpdateBookingResponseDto updateBooking(UpdateBookingRequestDto bookingRequestDto, Long bookingId);

    BookingDetailDto getBookingById(Long bookingId);

    List<BookingDetailDto> getBookingsByPassengerId(Long passengerId);

    List<BookingDetailDto> getBookingsByDriverId(Long driverId);

    UpdateBookingResponseDto updateBookingStatus(Long bookingId, String status);

    UpdateBookingResponseDto cancelBooking(Long bookingId);

    /** Internal/lightweight API for other services (e.g. Socket notifications). */
    Long getPassengerIdForBooking(Long bookingId);
}

