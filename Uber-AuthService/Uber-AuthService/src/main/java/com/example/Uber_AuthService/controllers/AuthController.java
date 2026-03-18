package com.example.Uber_AuthService.controllers;


import com.example.Uber_AuthService.dto.AuthRequestDto;
import com.example.Uber_AuthService.dto.AuthResponseDto;
import com.example.Uber_AuthService.dto.PassengerDto;
import com.example.Uber_AuthService.dto.PassengerSignUpRequestDto;
import com.example.Uber_AuthService.services.AuthService;
import com.example.Uber_AuthService.services.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(AuthService authService, AuthenticationManager authenticationManager,
                          JwtService jwtService) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }



    @PostMapping("/signup/passenger")
    public ResponseEntity<PassengerDto> signup(@RequestBody PassengerSignUpRequestDto passengerSignUpRequestDto) {
        PassengerDto passengerDto = authService.signUpPassenger(passengerSignUpRequestDto);
        return new ResponseEntity<>(passengerDto, HttpStatus.CREATED);
    }


    @PostMapping("/signin/passenger")
    public ResponseEntity<AuthResponseDto> signin(@RequestBody AuthRequestDto  authRequestDto ,
                                                  HttpServletResponse response) {

        Authentication authentication = authenticationManager.authenticate(new
                UsernamePasswordAuthenticationToken(authRequestDto.getEmail(), authRequestDto.getPassword()));

        if(authentication.isAuthenticated()) {
            String jwtToken = jwtService.createToken("Abc@gmail");
        }

        return new ResponseEntity<>(AuthResponseDto.builder().success(true).build(),HttpStatus.OK);



    }



}
