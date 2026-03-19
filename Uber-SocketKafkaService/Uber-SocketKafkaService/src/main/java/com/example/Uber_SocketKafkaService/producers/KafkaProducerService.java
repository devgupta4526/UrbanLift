package com.example.Uber_SocketKafkaService.producers;

import com.example.Uber_SocketKafkaService.dtos.RideRequestDto;
import com.example.Uber_SocketKafkaService.dtos.RideResponseDto;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaProducerService(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendRideRequest(RideRequestDto dto) {
        kafkaTemplate.send("ride-request-topic", dto);
    }

    public void sendRideResponse(RideResponseDto dto) {
        kafkaTemplate.send("ride-response-topic", dto);
    }
}