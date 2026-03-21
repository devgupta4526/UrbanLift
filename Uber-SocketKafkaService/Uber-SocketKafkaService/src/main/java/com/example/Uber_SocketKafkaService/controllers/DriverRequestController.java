package com.example.Uber_SocketKafkaService.controllers;


import com.example.Uber_SocketKafkaService.dtos.NotificationEventDto;
import com.example.Uber_SocketKafkaService.dtos.RideRequestDto;
import com.example.Uber_SocketKafkaService.dtos.RideResponseDto;
import com.example.Uber_SocketKafkaService.dtos.UpdateBookingRequestDto;
import com.example.Uber_SocketKafkaService.producers.KafkaProducerService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/socket")
public class DriverRequestController {

    private static final Logger logger = LoggerFactory.getLogger(DriverRequestController.class);
    private final KafkaProducerService kafkaProducerService;
    private final RestTemplate restTemplate;
    public DriverRequestController(KafkaProducerService kafkaProducerService, RestTemplate restTemplate) {
        this.kafkaProducerService = kafkaProducerService;
        this.restTemplate = restTemplate;
    }

    @PostMapping("/newride")
    public ResponseEntity<Boolean> raiseRideRequest(@RequestBody RideRequestDto requestDto) {

        logger.info("Sending ride request to Kafka: {}", requestDto);
        kafkaProducerService.sendRideRequest(requestDto);

        return ResponseEntity.ok(true);
    }


    @MessageMapping("/rideResponse/{userId}")
    public void rideResponseHandler(
            @DestinationVariable String userId,
            RideResponseDto responseDto) {

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
                UpdateBookingRequestDto updateRequest = UpdateBookingRequestDto.builder()
                        .status("SCHEDULED")
                        .driverId(Optional.of(driverId))
                        .build();

                String url = "http://localhost:8001/api/v1/booking/" + responseDto.getBookingId();
                logger.info("Updating booking {} with driver {}", responseDto.getBookingId(), driverId);
                restTemplate.postForEntity(url, updateRequest, String.class);

                // Send notification event
                NotificationEventDto notificationEvent = new NotificationEventDto();
                notificationEvent.setEventType("DRIVER_ASSIGNED");
                notificationEvent.setUserId(1L); // TODO: Get passenger ID from booking
                notificationEvent.setUserType("PASSENGER");
                Map<String, Object> payload = new HashMap<>();
                payload.put("bookingId", responseDto.getBookingId());
                payload.put("driverId", driverId);
                notificationEvent.setPayload(payload);
                kafkaProducerService.sendNotificationEvent(notificationEvent);
            }
        }
    }
}
