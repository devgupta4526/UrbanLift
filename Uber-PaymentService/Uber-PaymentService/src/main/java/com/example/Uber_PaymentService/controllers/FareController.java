package com.example.Uber_PaymentService.controllers;

import com.example.Uber_PaymentService.dtos.FareEstimateDto;
import com.example.Uber_PaymentService.dtos.FareEstimateRequestDto;
import com.example.Uber_PaymentService.services.FareCalculationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/fare")
public class FareController {

    private final FareCalculationService fareCalculationService;

    public FareController(FareCalculationService fareCalculationService) {
        this.fareCalculationService = fareCalculationService;
    }

    @PostMapping("/estimate")
    public ResponseEntity<FareEstimateDto> estimateFare(@Valid @RequestBody FareEstimateRequestDto request) {
        FareEstimateDto estimate = fareCalculationService.estimateFare(request);
        return ResponseEntity.ok(estimate);
    }
}