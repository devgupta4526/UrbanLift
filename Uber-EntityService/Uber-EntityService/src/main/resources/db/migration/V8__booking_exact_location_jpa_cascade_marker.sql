-- V8: JPA-only change — Booking.startLocation / endLocation now use cascade PERSIST.
-- No DDL: FK columns booking.start_location_id and booking.end_location_id already exist (V6).
-- This migration records the release in Flyway history alongside the entity JAR update.

SELECT 1 AS v8_booking_exact_location_cascade_ok;
