package com.example.Uber_BookingService.repositories;

import com.example.Uber_BookingService.dtos.TripRatingSummaryDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TripRatingRepository {
    private final JdbcTemplate jdbcTemplate;

    public TripRatingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsertPassengerToDriver(Long bookingId, Long passengerId, Integer score, String comment) {
        jdbcTemplate.update(
                """
                INSERT INTO trip_rating
                    (booking_id, direction, from_user_id, to_user_id, score, comment, created_at, updated_at)
                SELECT ?, 'PASSENGER_TO_DRIVER', ?, b.driver_id, ?, ?, NOW(), NOW()
                FROM booking b
                WHERE b.id = ? AND b.driver_id IS NOT NULL
                ON DUPLICATE KEY UPDATE
                    score = VALUES(score),
                    comment = VALUES(comment),
                    updated_at = NOW()
                """,
                bookingId, passengerId, score, comment, bookingId
        );
    }

    public void upsertDriverToPassenger(Long bookingId, Long driverId, Integer score, String comment) {
        jdbcTemplate.update(
                """
                INSERT INTO trip_rating
                    (booking_id, direction, from_user_id, to_user_id, score, comment, created_at, updated_at)
                SELECT ?, 'DRIVER_TO_PASSENGER', ?, b.passenger_id, ?, ?, NOW(), NOW()
                FROM booking b
                WHERE b.id = ? AND b.passenger_id IS NOT NULL
                ON DUPLICATE KEY UPDATE
                    score = VALUES(score),
                    comment = VALUES(comment),
                    updated_at = NOW()
                """,
                bookingId, driverId, score, comment, bookingId
        );
    }

    public TripRatingSummaryDto getSummary(Long bookingId) {
        return jdbcTemplate.query(
                """
                SELECT
                    MAX(CASE WHEN direction='PASSENGER_TO_DRIVER' THEN score END) AS p2d_score,
                    MAX(CASE WHEN direction='PASSENGER_TO_DRIVER' THEN comment END) AS p2d_comment,
                    MAX(CASE WHEN direction='DRIVER_TO_PASSENGER' THEN score END) AS d2p_score,
                    MAX(CASE WHEN direction='DRIVER_TO_PASSENGER' THEN comment END) AS d2p_comment
                FROM trip_rating
                WHERE booking_id = ?
                """,
                rs -> {
                    if (!rs.next()) return TripRatingSummaryDto.builder().build();
                    return TripRatingSummaryDto.builder()
                            .passengerToDriverScore((Integer) rs.getObject("p2d_score"))
                            .passengerToDriverComment(rs.getString("p2d_comment"))
                            .driverToPassengerScore((Integer) rs.getObject("d2p_score"))
                            .driverToPassengerComment(rs.getString("d2p_comment"))
                            .build();
                },
                bookingId
        );
    }
}
