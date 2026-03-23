package com.example.Uber_DriverService.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverSignUpRequestDto {

    @NotBlank @Size(max = 100)
    private String firstName;
    @NotBlank @Size(max = 100)
    private String lastName;
    @NotBlank @Email @Size(max = 255)
    private String email;
    @NotBlank @Size(max = 32)
    private String phoneNumber;
    @NotBlank @Size(min = 8, max = 128)
    private String password;
    @Size(max = 500)
    private String address;
    @NotBlank @Size(max = 64)
    private String licenseNumber;
    @NotBlank @Size(max = 32)
    private String aadharNumber;
    @Size(max = 100)
    private String activeCity;
    @Valid
    private CarDto car;
}
