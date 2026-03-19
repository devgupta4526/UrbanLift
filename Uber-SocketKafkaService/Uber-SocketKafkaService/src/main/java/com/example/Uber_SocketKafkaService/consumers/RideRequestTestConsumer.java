package com.example.Uber_SocketKafkaService.consumers;

import com.example.Uber_SocketKafkaService.dtos.RideRequestDto;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class RideRequestTestConsumer {

    private CountDownLatch latch = new CountDownLatch(1);
    private AtomicReference<RideRequestDto> receivedMessage = new AtomicReference<>();

    // Use a fixed group but force offset reset to latest so it only
    // picks up messages produced AFTER the consumer starts
    @KafkaListener(
            topics = "ride-request-topic",
            groupId = "test-group",
            containerFactory = "kafkaListenerContainerFactory",
            properties = {"auto.offset.reset=latest"}   // ✅ only consume NEW messages
    )
    public void consume(RideRequestDto dto) {
        System.out.println("✅ Test consumer received: " + dto);
        receivedMessage.set(dto);
        latch.countDown();
    }

    public CountDownLatch getLatch() {
        return latch;
    }

    public AtomicReference<RideRequestDto> getReceivedMessage() {
        return receivedMessage;
    }

    public void reset() {
        latch = new CountDownLatch(1);
        receivedMessage.set(null);
    }
}