package com.example.Uber_NotificationService.consumers;

import com.example.Uber_NotificationService.dtos.NotificationEventDto;
import com.example.Uber_NotificationService.services.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);

    private final NotificationService notificationService;

    public KafkaConsumerService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(
            topics = "notification-events-topic",
            groupId = "notification-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeNotificationEvent(NotificationEventDto event) {
        logger.info("Received notification event: {}", event);
        notificationService.sendNotification(event);
    }
}