package com.example.Uber_BookingService.repositories;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BookingIdempotencyRepository {

    private final JdbcTemplate jdbcTemplate;

    public BookingIdempotencyRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long findBookingId(Long passengerId, String idempotencyKey) {
        try {
            return jdbcTemplate.queryForObject(
                    """
                    SELECT booking_id
                    FROM booking_idempotency
                    WHERE passenger_id = ? AND idempotency_key = ?
                    """,
                    Long.class,
                    passengerId,
                    idempotencyKey
            );
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    public int insertMapping(Long passengerId, String idempotencyKey, Long bookingId) {
        return jdbcTemplate.update(
                """
                INSERT INTO booking_idempotency (passenger_id, idempotency_key, booking_id, created_at)
                VALUES (?, ?, ?, NOW())
                """,
                passengerId,
                idempotencyKey,
                bookingId
        );
    }
}
