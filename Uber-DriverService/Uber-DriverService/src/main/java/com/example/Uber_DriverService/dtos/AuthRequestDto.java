package com.example.Uber_DriverService.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequestDto {
    @NotBlank @Email
    private String email;
    @NotBlank
    private String password;
}
