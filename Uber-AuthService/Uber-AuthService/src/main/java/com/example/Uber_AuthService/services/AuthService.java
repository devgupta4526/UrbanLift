package com.example.Uber_AuthService.services;

import com.example.Uber_AuthService.dto.PassengerDto;
import com.example.Uber_AuthService.dto.PassengerSignUpRequestDto;
import com.example.Uber_AuthService.repositories.PassengerRepository;
import com.example.Uber_AuthService.util.EmailNormalizer;
import com.example.Uber_EntityService.Models.Passenger;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private PassengerRepository passengerRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    AuthService(PassengerRepository passengerRepository, BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.passengerRepository = passengerRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;

    }

    public PassengerDto signUpPassenger(PassengerSignUpRequestDto passengerSignUpRequestDto) {
        String email = EmailNormalizer.normalize(passengerSignUpRequestDto.getEmail());
        if (passengerRepository.findPassengerByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }
        Passenger passenger = Passenger.builder()
                .email(email)
                .firstName(trimToNull(passengerSignUpRequestDto.getFirstName()))
                .lastName(trimToNull(passengerSignUpRequestDto.getLastName()))
                .password(bCryptPasswordEncoder.encode(passengerSignUpRequestDto.getPassword()))
                .address(trimToNull(passengerSignUpRequestDto.getAddress()))
                .phoneNumber(trimToNull(passengerSignUpRequestDto.getPhoneNumber()))
                .build();
        try {
            Passenger newPassenger = passengerRepository.save(passenger);
            return PassengerDto.from(newPassenger);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered", ex);
        }
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
