package com.example.Uber_EntityService.Models;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "wallet")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet extends BaseModel {

    private Long userId;

    @Enumerated(EnumType.STRING)
    private UserType userType;

    private BigDecimal balance;

    private String currency;

    public enum UserType {
        PASSENGER, DRIVER
    }
}