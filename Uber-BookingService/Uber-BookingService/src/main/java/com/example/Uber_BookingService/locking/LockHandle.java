package com.example.Uber_BookingService.locking;

public record LockHandle(String key, String token, boolean redis) {
}
