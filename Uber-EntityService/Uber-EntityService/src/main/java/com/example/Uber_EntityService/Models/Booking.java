package com.example.Uber_EntityService.Models;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking extends BaseModel{

    @Enumerated(value = EnumType.STRING)
    private BookingStatus bookingStatus;

    @Column(nullable = false)
    @Temporal(TemporalType.DATE)
    private Date bookingDate;

    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date startTime;

    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date endTime;

    private long totalDistance;

    @ManyToOne(fetch = FetchType.LAZY)
    private Passenger passenger;

    @ManyToOne(fetch = FetchType.LAZY)
    private Driver driver;

    @OneToOne
    private ExactLocation startLocation;
    @OneToOne
    private ExactLocation endLocation;






}
