package com.example.Uber_BookingService.consumers;


import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import com.example.Uber_BookingService.dtos.RideResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class KafkaConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);

    @KafkaListener(
            topics = "ride-response-topic",
            groupId = "booking-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeRideResponse(RideResponseDto responseDto) {
//        System.out.println("🔔 Ride response received in Booking Service: " + responseDto);
        logger.info("Ride response received in Booking Service: {}", responseDto);

        // TODO: handle booking confirmation logic here
    }
}