package com.example.Uber_BookingService.controllers;

import com.example.Uber_BookingService.dtos.*;
import com.example.Uber_BookingService.services.BookingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

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

    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingDetailDto> getBooking(@PathVariable Long bookingId) {
        return ResponseEntity.ok(bookingService.getBookingById(bookingId));
    }

    @PostMapping("/{bookingId}")
    public ResponseEntity<UpdateBookingResponseDto> updateBooking(@RequestBody UpdateBookingRequestDto requestDto, @PathVariable Long bookingId) {
        return new ResponseEntity<>(bookingService.updateBooking(requestDto, bookingId), HttpStatus.OK);
    }

    @GetMapping("/passenger/{passengerId}")
    public ResponseEntity<List<BookingDetailDto>> getBookingsByPassenger(@PathVariable Long passengerId) {
        return ResponseEntity.ok(bookingService.getBookingsByPassengerId(passengerId));
    }

    @GetMapping("/driver/{driverId}")
    public ResponseEntity<List<BookingDetailDto>> getBookingsByDriver(@PathVariable Long driverId) {
        return ResponseEntity.ok(bookingService.getBookingsByDriverId(driverId));
    }

    @PutMapping("/{bookingId}/status")
    public ResponseEntity<UpdateBookingResponseDto> updateStatus(@PathVariable Long bookingId, @RequestParam String status) {
        return ResponseEntity.ok(bookingService.updateBookingStatus(bookingId, status));
    }

    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<UpdateBookingResponseDto> cancelBooking(@PathVariable Long bookingId) {
        return ResponseEntity.ok(bookingService.cancelBooking(bookingId));
    }


}

