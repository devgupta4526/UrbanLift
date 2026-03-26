package com.example.Uber_SocketKafkaService.controllers;


import com.example.Uber_SocketKafkaService.dtos.NotificationEventDto;
import com.example.Uber_SocketKafkaService.dtos.RideRequestDto;
import com.example.Uber_SocketKafkaService.dtos.RideResponseDto;
import com.example.Uber_SocketKafkaService.producers.KafkaProducerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/socket")
public class DriverRequestController {

    private static final Logger logger = LoggerFactory.getLogger(DriverRequestController.class);
    private final KafkaProducerService kafkaProducerService;
    private final RestTemplate restTemplate;

    @Value("${urbanlift.booking-service-base-url:http://localhost:8001}")
    private String bookingServiceBaseUrl;

    public DriverRequestController(KafkaProducerService kafkaProducerService, RestTemplate restTemplate) {
        this.kafkaProducerService = kafkaProducerService;
        this.restTemplate = restTemplate;
    }

    @PostMapping("/newride")
    public ResponseEntity<Boolean> raiseRideRequest(@Valid @RequestBody RideRequestDto requestDto) {

        logger.info("Sending ride request to Kafka: {}", requestDto);
        kafkaProducerService.sendRideRequest(requestDto);

        return ResponseEntity.ok(true);
    }


    @MessageMapping("/rideResponse/{userId}")
    public void rideResponseHandler(
            @DestinationVariable String userId,
            RideResponseDto responseDto) {

        if (responseDto == null || responseDto.getBookingId() == null) {
            logger.warn("Ignoring invalid ride response: payload/bookingId missing");
            return;
        }
        logger.info("Received driver response: {}", responseDto);

        // Send event to Kafka
        kafkaProducerService.sendRideResponse(responseDto);

        // Only call booking service when driver accepts
        if (Boolean.TRUE.equals(responseDto.getResponse())) {
            Long driverId = responseDto.getDriverId();
            if (driverId == null && userId != null) {
                try {
                    driverId = Long.parseLong(userId);
                } catch (NumberFormatException ignored) {
                }
            }

            if (driverId != null) {
                logger.info(
                        "Driver accepted booking {} with driverId {}. Publishing Kafka event; Booking service consumer will apply assignment.",
                        responseDto.getBookingId(),
                        driverId
                );

                Long passengerId = fetchPassengerIdForBooking(responseDto.getBookingId());
                if (passengerId != null) {
                    NotificationEventDto notificationEvent = new NotificationEventDto();
                    notificationEvent.setEventType("DRIVER_ASSIGNED");
                    notificationEvent.setUserId(passengerId);
                    notificationEvent.setUserType("PASSENGER");
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("bookingId", responseDto.getBookingId());
                    payload.put("driverId", driverId);
                    notificationEvent.setPayload(payload);
                    kafkaProducerService.sendNotificationEvent(notificationEvent);
                } else {
                    logger.warn("Skipping DRIVER_ASSIGNED notification: could not resolve passenger for booking {}",
                            responseDto.getBookingId());
                }
            }
        }
    }

    /**
     * REMOVED: {@code catch (Exception e)} — too broad; could hide programming errors.
     * Replaced with {@link RestClientException} for HTTP/client failures from {@link RestTemplate}.
     */
    private Long fetchPassengerIdForBooking(Long bookingId) {
        try {
            String url = bookingServiceBaseUrl.replaceAll("/$", "")
                    + "/api/v1/booking/" + bookingId + "/passenger-id";
            @SuppressWarnings("unchecked")
            Map<String, Object> body = restTemplate.getForObject(url, Map.class);
            if (body == null || !body.containsKey("passengerId")) {
                logger.warn("No passengerId in booking service response for booking {}", bookingId);
                return null;
            }
            Object raw = body.get("passengerId");
            if (raw instanceof Number n) {
                return n.longValue();
            }
            return null;
        } catch (RestClientException e) {
            logger.error("Could not load passenger id for booking {} (booking service HTTP error)", bookingId, e);
            return null;
        }
    }
}
