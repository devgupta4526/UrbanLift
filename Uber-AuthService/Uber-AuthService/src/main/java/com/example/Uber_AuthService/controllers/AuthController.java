package com.example.Uber_AuthService.controllers;


import com.example.Uber_AuthService.dto.PassengerDto;
import com.example.Uber_AuthService.dto.PassengerSignUpRequestDto;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @PostMapping("/signup/passenger")
    public PassengerDto signup(@RequestBody PassengerSignUpRequestDto passengerSignUpRequestDto) {
        return null;
    }

}
