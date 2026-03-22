package com.example.Uber_DriverService.dtos;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverSignUpRequestDto {
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String password;
    private String address;
    private String licenseNumber;
    private String aadharNumber;
    private String activeCity;
    private CarDto car;
}
