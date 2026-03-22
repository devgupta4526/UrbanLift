package com.example.Uber_PaymentService.controllers;

import com.example.Uber_PaymentService.dtos.BookingSummaryDto;
import com.example.Uber_PaymentService.dtos.RideInvoiceDto;
import com.example.Uber_PaymentService.services.BillingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/billing")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @GetMapping("/ride/{bookingId}")
    public ResponseEntity<RideInvoiceDto> getRideInvoice(@PathVariable Long bookingId) {
        RideInvoiceDto invoice = billingService.getRideInvoice(bookingId);
        return ResponseEntity.ok(invoice);
    }

    @GetMapping("/history")
    public ResponseEntity<List<BookingSummaryDto>> getBillingHistory(@RequestParam Long userId, @RequestParam String userType) {
        List<BookingSummaryDto> history = billingService.getBillingHistory(userId, userType);
        return ResponseEntity.ok(history);
    }
}