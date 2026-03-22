package com.example.Uber_PaymentService.consumers;

import com.example.Uber_PaymentService.dtos.BookingCompletedEventDto;
import com.example.Uber_PaymentService.services.PaymentGatewayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);

    private final PaymentGatewayService paymentGatewayService;

    public KafkaConsumerService(PaymentGatewayService paymentGatewayService) {
        this.paymentGatewayService = paymentGatewayService;
    }

    @KafkaListener(
            topics = "booking-completed-topic",
            groupId = "payment-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeBookingCompleted(BookingCompletedEventDto event) {
        logger.info("Received booking completed event: {}", event);

        // Process payment for completed booking
        paymentGatewayService.processPaymentForCompletedBooking(event);
    }
}