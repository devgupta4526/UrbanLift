package com.example.Uber_AuthService.services;

import com.example.Uber_AuthService.dto.PassengerDto;
import com.example.Uber_AuthService.dto.PassengerSignUpRequestDto;
import com.example.Uber_AuthService.repositories.PassengerRepository;
import com.example.Uber_EntityService.Models.Passenger;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private PassengerRepository passengerRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    AuthService(PassengerRepository passengerRepository, BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.passengerRepository = passengerRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;

    }
    public PassengerDto signUpPassenger(PassengerSignUpRequestDto passengerSignUpRequestDto) {
        Passenger passenger = Passenger.builder()
                .email(passengerSignUpRequestDto.getEmail())
                .firstName(passengerSignUpRequestDto.getFirstName())
                .lastName(passengerSignUpRequestDto.getLastName())
                .password(bCryptPasswordEncoder.encode(passengerSignUpRequestDto.getPassword()))
                .address(passengerSignUpRequestDto.getAddress())
                .phoneNumber(passengerSignUpRequestDto.getPhoneNumber())
                .build();
        Passenger newPassenger = passengerRepository.save(passenger);

        return PassengerDto.from(newPassenger);

    }
}
