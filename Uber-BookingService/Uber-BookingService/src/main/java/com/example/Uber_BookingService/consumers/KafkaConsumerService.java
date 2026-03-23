package com.example.Uber_BookingService.consumers;


import com.example.Uber_BookingService.dtos.RideResponseDto;
import com.example.Uber_BookingService.dtos.UpdateBookingRequestDto;
import com.example.Uber_BookingService.services.BookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KafkaConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);

    private final BookingService bookingService;

    public KafkaConsumerService(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * Idempotent with HTTP path: if driver already assigned, update may still run with same data.
     */
    @KafkaListener(
            topics = "ride-response-topic",
            groupId = "booking-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeRideResponse(RideResponseDto responseDto) {
        logger.info("Ride response received in Booking Service: {}", responseDto);

        if (!Boolean.TRUE.equals(responseDto.getResponse())) {
            return;
        }
        if (responseDto.getBookingId() == null) {
            logger.warn("Ride accept missing bookingId");
            return;
        }
        Long driverId = responseDto.getDriverId();
        if (driverId == null) {
            logger.warn("Ride accept missing driverId for booking {}", responseDto.getBookingId());
            return;
        }
        try {
            UpdateBookingRequestDto update = UpdateBookingRequestDto.builder()
                    .status("SCHEDULED")
                    .driverId(driverId)
                    .build();
            bookingService.updateBooking(update, responseDto.getBookingId());
            logger.info("Booking {} updated from Kafka ride-response with driver {}", responseDto.getBookingId(), driverId);
        } catch (Exception e) {
            // Intentionally catch all: listener must not throw unhandled errors without a DLQ/retry policy.
            logger.error("Failed to apply ride response for booking {}", responseDto.getBookingId(), e);
        }
    }
}