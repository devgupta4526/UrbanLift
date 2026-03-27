package com.example.Uber_BookingService.controllers;

import com.example.Uber_BookingService.dtos.*;
import com.example.Uber_BookingService.services.BookingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/booking")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {

        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<CreateBookingResponseDto> createBooking(
            @RequestBody CreateBookingDto createBookingDto,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) throws IOException {
        return new ResponseEntity<>(bookingService.createBooking(createBookingDto, idempotencyKey), HttpStatus.CREATED);
    }

    @GetMapping("/open-assigning")
    public ResponseEntity<List<BookingDetailDto>> getOpenAssigningBookings() {
        return ResponseEntity.ok(bookingService.getOpenAssigningBookings());
    }

    @GetMapping("/passenger/{passengerId}")
    public ResponseEntity<List<BookingDetailDto>> getBookingsByPassenger(@PathVariable Long passengerId) {
        return ResponseEntity.ok(bookingService.getBookingsByPassengerId(passengerId));
    }

    @GetMapping("/driver/{driverId}")
    public ResponseEntity<List<BookingDetailDto>> getBookingsByDriver(@PathVariable Long driverId) {
        return ResponseEntity.ok(bookingService.getBookingsByDriverId(driverId));
    }

    @GetMapping("/{bookingId:\\d+}/passenger-id")
    public ResponseEntity<Map<String, Long>> getPassengerIdForBooking(@PathVariable Long bookingId) {
        Long passengerId = bookingService.getPassengerIdForBooking(bookingId);
        return ResponseEntity.ok(Map.of("passengerId", passengerId));
    }

    @GetMapping("/{bookingId:\\d+}")
    public ResponseEntity<BookingDetailDto> getBooking(@PathVariable Long bookingId) {
        return ResponseEntity.ok(bookingService.getBookingById(bookingId));
    }

    @PostMapping("/{bookingId:\\d+}")
    public ResponseEntity<UpdateBookingResponseDto> updateBooking(@RequestBody UpdateBookingRequestDto requestDto, @PathVariable Long bookingId) {
        return new ResponseEntity<>(bookingService.updateBooking(requestDto, bookingId), HttpStatus.OK);
    }

    @PutMapping("/{bookingId:\\d+}/status")
    public ResponseEntity<UpdateBookingResponseDto> updateStatus(@PathVariable Long bookingId, @RequestParam String status) {
        return ResponseEntity.ok(bookingService.updateBookingStatus(bookingId, status));
    }

    @PostMapping("/{bookingId:\\d+}/cancel")
    public ResponseEntity<UpdateBookingResponseDto> cancelBooking(@PathVariable Long bookingId) {
        return ResponseEntity.ok(bookingService.cancelBooking(bookingId));
    }

    @PostMapping("/{bookingId:\\d+}/rating/driver")
    public ResponseEntity<Void> rateDriver(@PathVariable Long bookingId, @RequestBody @jakarta.validation.Valid TripRatingRequestDto request) {
        bookingService.rateDriver(bookingId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{bookingId:\\d+}/rating/passenger")
    public ResponseEntity<Void> ratePassenger(@PathVariable Long bookingId, @RequestBody @jakarta.validation.Valid TripRatingRequestDto request) {
        bookingService.ratePassenger(bookingId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{bookingId:\\d+}/rating")
    public ResponseEntity<TripRatingSummaryDto> getTripRatings(@PathVariable Long bookingId) {
        return ResponseEntity.ok(bookingService.getTripRatings(bookingId));
    }


}

