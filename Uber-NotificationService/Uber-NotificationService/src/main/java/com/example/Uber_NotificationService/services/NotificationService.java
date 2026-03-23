package com.example.Uber_NotificationService.services;

import com.example.Uber_NotificationService.dtos.NotificationEventDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    public void sendNotification(NotificationEventDto event) {
        if (event == null) {
            throw new IllegalArgumentException("event is required");
        }
        logger.info("Sending notification for event: {} to user: {}", event.getEventType(), event.getUserId());

        // Mock implementation - in real world, integrate with FCM, Twilio, etc.
        switch (event.getEventType()) {
            case "DRIVER_ASSIGNED":
                sendPushNotification(event.getUserId(), "Driver assigned", "Your driver is on the way!");
                break;
            case "CAB_ARRIVED":
                sendPushNotification(event.getUserId(), "Driver arrived", "Your driver has arrived!");
                break;
            case "RIDE_STARTED":
                sendPushNotification(event.getUserId(), "Ride started", "Your ride has started!");
                break;
            case "RIDE_COMPLETED":
                sendPushNotification(event.getUserId(), "Ride completed", "Your ride has been completed!");
                sendEmail(event.getUserId(), "Ride Receipt", "Your ride receipt...");
                break;
            case "RIDE_CANCELLED":
                sendPushNotification(event.getUserId(), "Ride cancelled", "Your ride has been cancelled.");
                break;
            default:
                logger.warn("Unknown event type: {}", event.getEventType());
        }
    }

    private void sendPushNotification(Long userId, String title, String message) {
        logger.info("Mock: Sending push notification to user {}: {} - {}", userId, title, message);
        // Integrate with FCM
    }

    private void sendSms(Long userId, String message) {
        logger.info("Mock: Sending SMS to user {}: {}", userId, message);
        // Integrate with Twilio
    }

    private void sendEmail(Long userId, String subject, String body) {
        logger.info("Mock: Sending email to user {}: {} - {}", userId, subject, body);
        // Integrate with SendGrid
    }
}
