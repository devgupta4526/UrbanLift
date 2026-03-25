package com.example.Uber_AuthService.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponseDto {
    private Boolean success;
    /** Present after sign-in and successful session validation (from JWT). */
    private Long passengerId;
    /** Passenger email (subject) when available. */
    private String email;
}
