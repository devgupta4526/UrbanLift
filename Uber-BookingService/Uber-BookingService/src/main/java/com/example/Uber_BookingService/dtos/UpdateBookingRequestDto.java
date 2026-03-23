package com.example.Uber_BookingService.dtos;

import lombok.*;

/**
 * Use a plain {@code Long} for {@code driverId} in JSON APIs — not {@link java.util.Optional}.
 * Jackson often leaves {@code Optional} fields null or empty when the body is {@code "driverId": 1},
 * so the booking update ran with {@code driver = null} and the response showed no driver.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateBookingRequestDto {

    private String status;
    /** Nullable: omit or null to leave driver unchanged on update (if you add that behavior later). */
    private Long driverId;

}