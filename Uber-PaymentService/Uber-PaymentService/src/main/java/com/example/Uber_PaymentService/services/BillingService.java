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
            throw new RuntimeException("Payment not found for booking: " + bookingId);
        }

        Payment payment = payments.get(0);

        BigDecimal totalFare   = payment.getAmount();
        BigDecimal commission  = totalFare.multiply(BigDecimal.valueOf(0.1))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal driverEarnings = totalFare.subtract(commission);

        RideInvoiceDto dto = new RideInvoiceDto();
        dto.setBookingId(bookingId);
        dto.setBaseFare(BigDecimal.valueOf(50.0));
        dto.setDistanceFare(BigDecimal.valueOf(100.0));
        dto.setTimeFare(BigDecimal.valueOf(20.0));
        dto.setSurgeMultiplier(BigDecimal.valueOf(1.2));
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