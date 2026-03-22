package com.example.Uber_EntityService.Models;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fare_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FareConfig extends BaseModel {

    @Enumerated(EnumType.STRING)
    private CarType carType;

    private BigDecimal baseFare;

    private BigDecimal perKmRate;

    private BigDecimal perMinRate;

    private BigDecimal surgeMultiplier;
}
