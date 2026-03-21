package com.example.Uber_BookingService.controllers;

import com.example.Uber_BookingService.dtos.*;
import com.example.Uber_BookingService.services.BookingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/booking")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {

        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<CreateBookingResponseDto> createBooking(@RequestBody CreateBookingDto createBookingDto) throws IOException {

        return new ResponseEntity<>(bookingService.createBooking(createBookingDto), HttpStatus.CREATED);
    }

    @PostMapping("/{bookingId}")
    public ResponseEntity<UpdateBookingResponseDto> updateBooking(@RequestBody UpdateBookingRequestDto requestDto, @PathVariable Long bookingId) {
        return new ResponseEntity<>(bookingService.updateBooking(requestDto, bookingId), HttpStatus.OK);
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingDetailDto> getBooking(@PathVariable Long bookingId) {
        return ResponseEntity.ok(bookingService.getBookingById(bookingId));
    }
}

