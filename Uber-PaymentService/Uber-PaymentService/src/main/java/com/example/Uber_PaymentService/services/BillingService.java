package com.example.Uber_PaymentService.services;

import com.example.Uber_EntityService.Models.Payment;
import com.example.Uber_PaymentService.dtos.BookingSummaryDto;
import com.example.Uber_PaymentService.dtos.RideInvoiceDto;
import com.example.Uber_PaymentService.repositories.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class BillingService {

    @Autowired
    private PaymentRepository paymentRepository;

    public RideInvoiceDto getRideInvoice(Long bookingId) {
        List<Payment> payments = paymentRepository.findByBookingId(bookingId);
        if (payments.isEmpty()) {
            throw new IllegalArgumentException("Payment not found for booking: " + bookingId);
        }

        Payment payment = payments.get(0);

        BigDecimal totalFare   = payment.getAmount();
        BigDecimal commission  = totalFare.multiply(BigDecimal.valueOf(0.1))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal driverEarnings = totalFare.subtract(commission);

        /*
         * Fare component breakdown is not persisted on Payment today. Until booking/fare-quote fields are stored,
         * derive a display-only split that sums to totalFare (replaces hardcoded 50/100/20/1.2 that ignored reality).
         */
        BigDecimal baseFare = totalFare.multiply(BigDecimal.valueOf(0.35)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal distanceFare = totalFare.multiply(BigDecimal.valueOf(0.45)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal timeFare = totalFare.subtract(baseFare).subtract(distanceFare).setScale(2, RoundingMode.HALF_UP);
        BigDecimal surgeMultiplier = BigDecimal.ONE;

        RideInvoiceDto dto = new RideInvoiceDto();
        dto.setBookingId(bookingId);
        dto.setBaseFare(baseFare);
        dto.setDistanceFare(distanceFare);
        dto.setTimeFare(timeFare);
        dto.setSurgeMultiplier(surgeMultiplier);
        dto.setTotalFare(totalFare);
        dto.setCommission(commission);
        dto.setDriverEarnings(driverEarnings);
        dto.setPaymentTime(payment.getCreatedAt());

        return dto;
    }

    public List<BookingSummaryDto> getBillingHistory(Long userId, String userType) {
        return List.of();
    }
}