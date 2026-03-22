package com.example.Uber_EntityService.Models;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment extends BaseModel {

    private Long bookingId;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private String gatewayOrderId;

    private String gatewayPaymentId;

    public enum PaymentStatus {
        PENDING, COMPLETED, FAILED, REFUNDED
    }
}
