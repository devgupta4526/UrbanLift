package com.example.Uber_AuthService.dto;


import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PassengerSignUpRequestDto {
    private String email;

    private String password;

    private String phoneNumber;

    private String name;

}
