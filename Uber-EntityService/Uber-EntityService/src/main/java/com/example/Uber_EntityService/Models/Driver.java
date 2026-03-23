package com.example.Uber_EntityService.Models;


import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.*;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Driver extends BaseModel{
    private String firstName;
    private String lastName;

    @Column(nullable = false, unique = true, length = 255)
    private String email;
    private String phoneNumber;
    private String password;
    private String address;

    @Column(nullable = false, unique = true)
    private String licenseNumber;
    @Column(nullable = false, unique = true)
    private String aadharNumber;

    @OneToOne(mappedBy = "driver", cascade = CascadeType.ALL)
    private Car car;

    @Enumerated(EnumType.STRING)
    private DriverApprovalStatus driverApprovalStatus;

    private String activeCity;
    @OneToOne
    private ExactLocation lastKnownLocation;
    @OneToOne
    private ExactLocation home;

    @DecimalMin(value = "0.00", message = "Rating must be grater than or equal to 0.00")
    @DecimalMax(value = "5.00", message = "Rating must be less than or equal to 5.00")
    private Double rating;

    private boolean isAvailable;

    @OneToMany(mappedBy = "driver")
    @Fetch(FetchMode.SUBSELECT)
    private List<Booking> bookings;



}
