package com.example.Uber_PaymentService.controllers;

import com.example.Uber_PaymentService.dtos.*;
import com.example.Uber_PaymentService.services.PaymentGatewayService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payment")
public class PaymentController {

    private final PaymentGatewayService paymentGatewayService;

    public PaymentController(PaymentGatewayService paymentGatewayService) {
        this.paymentGatewayService = paymentGatewayService;
    }

    @PostMapping("/initiate")
    public ResponseEntity<PaymentInitiationResponseDto> initiatePayment(@Valid @RequestBody PaymentInitiationDto request) {
        PaymentInitiationResponseDto response = paymentGatewayService.initiatePayment(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/confirm")
    public ResponseEntity<PaymentConfirmResponseDto> confirmPayment(@Valid @RequestBody PaymentConfirmDto request) {
        PaymentConfirmResponseDto response = paymentGatewayService.confirmPayment(request);
        return ResponseEntity.ok(response);
    }
}