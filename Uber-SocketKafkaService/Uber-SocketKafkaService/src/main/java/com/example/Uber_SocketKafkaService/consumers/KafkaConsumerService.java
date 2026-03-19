package com.example.Uber_SocketKafkaService.consumers;

import com.example.Uber_SocketKafkaService.dtos.RideRequestDto;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumerService {

    private final SimpMessagingTemplate messagingTemplate;

    public KafkaConsumerService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @KafkaListener(topics = "ride-request-topic")
    public void consumeRideRequest(RideRequestDto dto) {
        System.out.println("Received ride request: " + dto);

        // Push to all drivers
        messagingTemplate.convertAndSend("/topic/rideRequest", dto);
    }

}
