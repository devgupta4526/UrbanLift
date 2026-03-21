package com.example.Uber_BookingService.producers;

import com.example.Uber_BookingService.dtos.BookingCompletedEventDto;
import com.example.Uber_BookingService.dtos.NotificationEventDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerService.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaProducerService(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendBookingCompletedEvent(BookingCompletedEventDto event) {
        logger.info("Sending booking completed event: {}", event);
        kafkaTemplate.send("booking-completed-topic", event);
    }

    public void sendNotificationEvent(NotificationEventDto event) {
        logger.info("Sending notification event: {}", event);
        kafkaTemplate.send("notification-events-topic", event);
    }
}
