package com.example.Uber_PaymentService.services;

import com.example.Uber_EntityService.Models.Payment;
import com.example.Uber_PaymentService.dtos.*;
import com.example.Uber_PaymentService.producers.KafkaProducerService;
import com.example.Uber_PaymentService.repositories.PaymentRepository;
import com.example.Uber_PaymentService.repositories.WalletRepository;
import com.example.Uber_PaymentService.services.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class PaymentGatewayService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentGatewayService.class);

    @Value("${payment.gateway.mock:true}")
    private boolean mock;

    private final PaymentRepository paymentRepository;
    private final WalletRepository walletRepository;
    private final WalletService walletService;
    private final KafkaProducerService kafkaProducerService;

    public PaymentGatewayService(PaymentRepository paymentRepository, WalletRepository walletRepository, WalletService walletService, KafkaProducerService kafkaProducerService) {
        this.paymentRepository = paymentRepository;
        this.walletRepository = walletRepository;
        this.walletService = walletService;
        this.kafkaProducerService = kafkaProducerService;
    }

    public PaymentInitiationResponseDto initiatePayment(PaymentInitiationDto request) {
        if (request.getAmount() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (mock) {
            // Mock implementation
            PaymentInitiationResponseDto response = new PaymentInitiationResponseDto();
            response.setOrderId("mock_order_" + System.currentTimeMillis());
            response.setAmount(request.getAmount());
            response.setCurrency("INR");
            response.setStatus("CREATED");

            // Create payment record
            Payment payment = Payment.builder()
                    .bookingId(request.getBookingId())
                    .amount(BigDecimal.valueOf(request.getAmount()))
                    .status(Payment.PaymentStatus.PENDING)
                    .gatewayOrderId(response.getOrderId())
                    .build();
            payment = paymentRepository.save(payment);
            response.setPaymentId(payment.getId());

            return response;
        }
        // Real implementation would call Razorpay/Stripe API
        throw new UnsupportedOperationException("Real payment gateway not implemented");
    }

    @Transactional
    public PaymentConfirmResponseDto confirmPayment(PaymentConfirmDto confirmDto) {
        if (mock) {
            if (confirmDto.getPaymentId() == null) {
                throw new IllegalArgumentException("paymentId is required");
            }
            Payment payment = paymentRepository.findById(confirmDto.getPaymentId())
                    .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + confirmDto.getPaymentId()));
            payment.setStatus(Payment.PaymentStatus.COMPLETED);
            if (confirmDto.getRazorpayPaymentId() != null) {
                payment.setGatewayPaymentId(confirmDto.getRazorpayPaymentId());
            }
            paymentRepository.save(payment);

            PaymentConfirmResponseDto response = new PaymentConfirmResponseDto();
            response.setSuccess(true);
            response.setPaymentId(String.valueOf(payment.getId()));
            response.setStatus("COMPLETED");
            response.setAmount(payment.getAmount().doubleValue());
            return response;
        }
        // Real implementation
        throw new UnsupportedOperationException("Real payment gateway not implemented");
    }

    public void processPaymentForCompletedBooking(BookingCompletedEventDto event) {
        logger.info("Processing payment for completed booking: {}", event.getBookingId());

        // Create payment record
        Payment payment = Payment.builder()
                .bookingId(event.getBookingId())
                .amount(event.getFare())
                .status(Payment.PaymentStatus.COMPLETED)
                .build();
        payment = paymentRepository.save(payment);

        // Deduct from passenger wallet
        walletService.deductMoney(event.getPassengerId(), "PASSENGER", event.getFare().doubleValue());

        // Credit to driver wallet
        walletService.creditMoney(event.getDriverId(), "DRIVER", event.getFare().doubleValue());

        // Send payment completed event
        PaymentCompletedEventDto completedEvent = new PaymentCompletedEventDto();
        completedEvent.setBookingId(event.getBookingId());
        completedEvent.setPaymentId(payment.getId());
        completedEvent.setStatus("COMPLETED");

        kafkaProducerService.sendPaymentCompletedEvent(completedEvent);
    }
}