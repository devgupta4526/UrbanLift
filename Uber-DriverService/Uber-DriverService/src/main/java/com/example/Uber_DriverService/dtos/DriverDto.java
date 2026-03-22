package com.example.Uber_DriverService.dtos;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String address;
    private String licenseNumber;
    private String driverApprovalStatus;
    private String activeCity;
    private Boolean isAvailable;
    private Double rating;
    private CarDto car;
}
