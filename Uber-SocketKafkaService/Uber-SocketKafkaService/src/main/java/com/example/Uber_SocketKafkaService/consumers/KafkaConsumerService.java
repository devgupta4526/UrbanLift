package com.example.Uber_SocketKafkaService.consumers;

import com.example.Uber_SocketKafkaService.dtos.RideRequestDto;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class KafkaConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);

    private final SimpMessagingTemplate messagingTemplate;

    public KafkaConsumerService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @KafkaListener(
            topics = "ride-request-topic",
            groupId = "socket-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeRideRequest(RideRequestDto dto) {

        logger.info("Received ride request from Kafka: {}", dto);
        // Push to all drivers via WebSocket
        messagingTemplate.convertAndSend("/topic/rideRequest", dto);
    }
}